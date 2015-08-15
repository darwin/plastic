(ns plastic.worker.servant
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [react! dispatch dispatch-args]])
  (:require [plastic.worker.frame]
            [cognitect.transit :as transit]))

(defn ^:export dispatch-message [data]
  (let [reader (transit/reader :json)
        id (aget data "id")
        command (aget data "command")]
    (condp = command
      "dispatch" (dispatch-args id (transit/read reader (aget data "args"))))))

(defn process-message [message]
  (dispatch-message (.-data message)))

(set! (.-onmessage js/self) process-message)

(defn post-dispatch-message [id args]
  (let [writer (transit/writer :json)
        data (js-obj
               "id" id
               "command" "dispatch"
               "args" (transit/write writer args))]
    (if-not plastic.env.run-worker-on-main-thread
      (.postMessage js/self data)
      (plastic.main.servant.dispatch-message data))))

(defn ^:export dispatch-on-main [id event-v]
  (post-dispatch-message id event-v))