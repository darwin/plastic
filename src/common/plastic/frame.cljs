(ns plastic.frame
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time fancy-log]]
                   [plastic.common :refer [process]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [plastic.env :as env :include-macros true]
            [re-frame.frame :refer [process-event register-subscription-handler make-frame]]
            [re-frame.middleware :refer [trim-v]]
            [re-frame.scaffold :refer [register-base legacy-subscribe]]
            [plastic.globals :as globals]
            [clojure.string :as string]
            [plastic.util.helpers :as helpers]))

; -------------------------------------------------------------------------------------------------------------------

(defprotocol IFrameThread
  (-thread-id [o]))

(defn get-thread-id [context]
  (-thread-id context))

(defn get-thread-label [context]
  (string/upper-case (str (name (get-thread-id context)))))

(defn bench? [context]
  (env/or context :bench-processing))

(defn timing [context handler]
  (fn timing-handler [db v]
    (measure-time (bench? context) "PROCESS" [v]
      (handler db v))))

(defn common-middleware [context]
  (let [{:keys [frame]} context]
    [(partial timing context) (trim-v frame)]))

(defn register-handler
  ([context event-id handler-fn] (register-handler context event-id nil handler-fn))
  ([context event-id middleware handler-fn]
   (let [{:keys [frame]} context
         all-middleware [(common-middleware context) middleware]
         context-handler (partial handler-fn context)]
     (register-base frame event-id all-middleware context-handler))
   context))

(defn register-sub [context subscription-id handler-fn]
  (let [{:keys [frame]} context]
    (swap! frame #(register-subscription-handler % subscription-id handler-fn)))
  context)

(defn subscribe [context subscription-spec]
  (let [{:keys [frame db]} context]
    (legacy-subscribe frame db subscription-spec)))

; -------------------------------------------------------------------------------------------------------------------

(defn handle-event-and-report-exceptions [frame db event]
  (binding [globals/*current-event-stack* (conj globals/*current-event-stack* event)]
    (try
      (let [event-info (meta event)
            pre-fn (or (:pre event-info) identity)
            post-fn (or (:post event-info) identity)
            process-fn (partial process-event frame)]
        (-> db
          (pre-fn)
          (process-fn event)
          (post-fn)))
      (catch :default e
        (error (.-stack e))
        db))))

(defn process-events [context events db]
  (process events db
    (fn [db event]
      (let [{:keys [event-queue frame]} context
            prev-queue-content @event-queue
            event-info (meta event)
            final-fn (or (:final event-info) identity)]
        (vreset! event-queue [])
        (let [new-db (handle-event-and-report-exceptions @frame db event)
              result-db (process-events context @event-queue new-db)]
          (vreset! event-queue prev-queue-content)
          (final-fn result-db))))))

(defn is-ack-batch? [event]
  (keyword-identical? (first event) :ack-batch))

(defn handle-ack-batch [context event db]
  (let [[_ tape post pre] event]
    (->> db
      (pre)
      (process-events context tape)
      (post))))

(defn reset-if-changed! [context db-atom new-db]
  (when-not (identical? @db-atom new-db)
    (if (env/get context :log-app-db)
      (fancy-log "APP DB" new-db))
    (reset! db-atom new-db)))

(defn apply-event [context db event]
  {:pre [(not globals/*current-thread*)]}
  (binding [globals/*current-thread* (get-thread-label context)]
    (let [new-db (if (is-ack-batch? event)
                   (handle-ack-batch context event @db)
                   (process-events context [event] @db))]
      (reset-if-changed! context db new-db))))

(defn run-loop [context]
  (go-loop []
    (let [{:keys [event-chan db]} context
          event (<! event-chan)]
      (apply-event context db event))
    (recur))
  context)

(defn start-loop [context]
  (when-not (env/get context :dont-run-loops)
    (let [{:keys [frame]} context]
      (log "starting loop for" (get-thread-label context) @frame)
      (run-loop context))))

; -------------------------------------------------------------------------------------------------------------------

(defn dispatch* [context event & [post-handler pre-handler final-handler]]
  {:pre [context]}
  (let [{:keys [event-chan event-queue]} context
        info (if (or post-handler pre-handler final-handler)
               {:pre   pre-handler
                :post  post-handler
                :final final-handler})
        event-with-info (with-meta event info)]
    (if @event-queue
      (helpers/vupdate! event-queue conj event-with-info)
      (put! event-chan event-with-info)))
  nil)

; -------------------------------------------------------------------------------------------------------------------

(defn init-frame [context]
  (-> context
    (assoc :frame (atom (make-frame)))
    (assoc :event-queue (volatile! nil))                                                                              ; for processing dependent events
    (assoc :event-chan (chan))))