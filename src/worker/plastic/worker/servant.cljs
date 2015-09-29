(ns plastic.worker.servant
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [plastic.worker :refer [react! dispatch dispatch-args]])
  (:require [plastic.worker.frame]
            [cognitect.transit :as transit]))

(defonce reader (transit/reader :json))
(defonce writer (transit/writer :json))
(defonce benchmark? (or plastic.env.bench-transmit plastic.env.bench-worker-transmit))

(defn post-message [id args]
  (let [data (js-obj
               "id" id
               "command" "dispatch"
               "args" (measure-time benchmark? "TRANSMIT" ["encode args"] (transit/write writer args)))]
    (if-not plastic.env.run-worker-on-main-thread
      (.postMessage js/self data)
      (plastic.main.servant.process-message data))))

; -------------------------------------------------------------------------------------------------------------------

(defn ^:export process-message [data]
  (let [id (aget data "id")
        args (aget data "args")
        event (measure-time benchmark? "TRANSMIT" ["decode args"] (transit/read reader args))
        command (aget data "command")]
    (condp = command
      "dispatch" (dispatch-args id event))))

(defn ^:export dispatch-on-main [id event-v]
  (post-message id event-v))

; -------------------------------------------------------------------------------------------------------------------

(set! (.-onmessage js/self) #(process-message (.-data %)))
