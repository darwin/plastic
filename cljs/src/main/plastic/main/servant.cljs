(ns plastic.main.servant
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main.glue :refer [react! dispatch dispatch-args]])
  (:require [cognitect.transit :as transit]
            [plastic.main.frame :as frame]))

(defonce worker-script "/worker.js")
(defonce ^:dynamic worker nil)
(defonce ^:dynamic msg-id 0)

(defn next-msg-id! []
  (set! msg-id (inc msg-id))
  msg-id)

(defn dispatch-event [message]
  (let [reader (transit/reader :json)
        data (.-data message)
        command (aget data "command")
        id (aget data "id")]
    (condp = command
      "dispatch" (dispatch-args id (transit/read reader (aget data "args"))))))

(defn spawn-workers [base-path]
  (let [script-url (str base-path worker-script)
        new-worker (js/Worker. script-url)]
    (.addEventListener new-worker "message" dispatch-event)
    (set! worker new-worker)))

(defn post-dispatch-message [args id]
  (let [writer (transit/writer :json)
        data (js-obj
               "id" id
               "command" "dispatch"
               "args" (transit/write writer args))]
    (.postMessage worker data)))

(defn dispatch-on-worker
  ([event-v] (dispatch-on-worker event-v nil))
  ([event-v after-effect] (let [id (next-msg-id!)]
                            (if after-effect
                              (frame/register-after-effect id after-effect))
                            (post-dispatch-message event-v id))))