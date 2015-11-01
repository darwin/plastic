(ns plastic.servant
  (:require [plastic.logging :refer-macros [log info warn error group group-end measure-time fancy-log]]
            [cognitect.transit :as transit]
            [plastic.common :refer-macros [process]]
            [re-frame.utils :refer [reset-if-changed!]]
            [plastic.frame :refer [get-thread-id dispatch* get-thread-label with-pre-handler
                                   with-final-handler] :refer-macros [dispatch]]
            [plastic.env :as env :include-macros true]
            [plastic.util.helpers :as helpers]
            [plastic.globals :as globals]))

; -------------------------------------------------------------------------------------------------------------------

(def transit-label "TRANSIT")

(defn benchmark? [context]
  (env/or context :bench-transit))

; -------------------------------------------------------------------------------------------------------------------

(defn register-pending-msg! [context msg-id post-handler pre-handler]
  (let [{:keys [pending-msgs]} context]
    (helpers/vupdate! pending-msgs assoc msg-id {:pre-handler  pre-handler
                                                 :post-handler post-handler})))
(defn remove-pending-msg! [context msg-id]
  (let [{:keys [pending-msgs]} context
        result (get @pending-msgs msg-id)]
    (helpers/vupdate! pending-msgs dissoc msg-id)
    result))

; -------------------------------------------------------------------------------------------------------------------

(defn next-msg-id! [context]
  (let [counter (:last-msg-id context)]
    (helpers/vupdate! counter inc)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-socket [context name]
  (get-in context [:sockets name]))

(defn set-sockets [context sockets]
  (assoc context :sockets sockets))

; -------------------------------------------------------------------------------------------------------------------

(defn get-dispatch-tape [context recipient]
  (get @(:dispatch-tape context) recipient))

(defn dispatch-recording? [context recipient]
  (boolean (get-dispatch-tape context recipient)))

(defn start-dispatch-recording! [context recipient]
  {:pre [(not (dispatch-recording? context recipient))]}
  (helpers/vupdate! (:dispatch-tape context) assoc recipient []))

(defn stop-dispatch-recording! [context recipient]
  {:pre [(dispatch-recording? context recipient)]}
  (let [tap (:dispatch-tape context)
        res (get @tap recipient)]
    (helpers/vupdate! (:dispatch-tape context) dissoc recipient)
    res))

(defn record-event-to-dispatch-tape [context recipient event post-handler pre-handler]
  {:pre [(dispatch-recording? context recipient)]}
  (helpers/vupdate! (:dispatch-tape context) update recipient conj [event post-handler pre-handler]))

(defn get-dispatch-tape-events [tape]
  (map first tape))

(defn chain-dispatch-tape-handlers [tape index]
  (let [handlers (map #(nth % index) tape)]
    (fn [db]
      (process handlers db
        (fn [db handler]
          (handler db))))))

(defn get-dispatch-tape-chained-pre-handlers [tape]
  (chain-dispatch-tape-handlers tape 2))

(defn get-dispatch-tape-chained-post-handlers [tape]
  (chain-dispatch-tape-handlers tape 1))

; -------------------------------------------------------------------------------------------------------------------

(defn apply-handler [context db handler-selector]
  (if-let [handler (handler-selector context)]
    (handler db)
    db))

(defn ack [context event]
  (let [[msg-id tape] event
        msg (remove-pending-msg! context msg-id)
        pre-handler (fn [db] (apply-handler msg db :pre-handler))
        post-handler (fn [db] (apply-handler msg db :post-handler))]
    (dispatch* context [:ack-batch tape post-handler pre-handler])))

(declare post-message)

(defn pack-payload [context msg-id sender command args]
  (js-obj
    "msg-id" msg-id
    "sender" (name sender)
    "command" (name command)
    "args" (measure-time (benchmark? context) transit-label ["encode args"]
             (transit/write (:writer context) args))))

(defn unpack-payload [context payload]
  {:msg-id  (int (aget payload "msg-id"))
   :sender  (keyword (aget payload "sender"))
   :command (keyword (aget payload "command"))
   :event   (measure-time (benchmark? context) transit-label ["decode args"]
              (transit/read (:reader context) (aget payload "args")))})

(defn process-message [context payload]
  (binding [globals/*current-thread* (get-thread-label context)]
    (let [{:keys [msg-id sender command event]} (unpack-payload context payload)
          pre-handler (fn [db]
                        (start-dispatch-recording! context sender)
                        db)
          final-handler (fn [db]
                          (let [tape (stop-dispatch-recording! context sender)
                                events (get-dispatch-tape-events tape)
                                pre-fn (get-dispatch-tape-chained-pre-handlers tape)
                                post-fn (get-dispatch-tape-chained-post-handlers tape)]
                            (post-message context sender :ack [msg-id events] post-fn pre-fn))
                          db)]
      (case command
        :ack (ack context event)
        :dispatch (dispatch context (-> event
                                      (with-pre-handler pre-handler)
                                      (with-final-handler final-handler)))))))

(defn post-message* [context recipient msg-id command args]
  (let [payload (pack-payload context msg-id (get-thread-id context) command args)
        post-fn (get-socket context recipient)]
    (post-fn payload)))

(defn post-message [context recipient command args post-handler pre-handler]
  (let [msg-id (next-msg-id! context)]
    (register-pending-msg! context msg-id post-handler pre-handler)
    (post-message* context recipient msg-id command args)))

(defn dispatch-to [context recipient event]
  (let [info (meta event)
        post-handler (:post info)
        pre-handler (:pre info)
        event-without-meta (with-meta event nil)]
    (if (dispatch-recording? context recipient)
      (record-event-to-dispatch-tape context recipient event-without-meta post-handler pre-handler)
      (post-message context recipient :dispatch event-without-meta post-handler pre-handler))))

; -------------------------------------------------------------------------------------------------------------------

(defn init-servant [context]
  (-> context
    (assoc :reader (transit/reader :json))
    (assoc :writer (transit/writer :json))
    (assoc :last-msg-id (volatile! 0))
    (assoc :pending-msgs (volatile! {}))
    (assoc :dispatch-tape (volatile! {}))))