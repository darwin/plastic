(ns plastic.worker.servant
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker.glue :refer [react! dispatch dispatch-args]])
  (:require [plastic.worker.frame]
            [cognitect.transit :as transit]))

(defn dispatch-incoming-message [message]
  (let [reader (transit/reader :json)
        data (.-data message)
        id (aget data "id")
        command (aget data "command")]
    (condp = command
      "dispatch" (dispatch-args id (transit/read reader (aget data "args"))))))

(set! (.-onmessage js/self) dispatch-incoming-message)

(defn post-dispatch-message [id args]
  (let [writer (transit/writer :json)
        data (js-obj
               "id" id
               "command" "dispatch"
               "args" (transit/write writer args))]
    (.postMessage js/self data)))

(defn dispatch-on-main [id event-v]
  (post-dispatch-message id event-v))