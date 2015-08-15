(ns plastic.main.servant
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch dispatch-args]])
  (:require [cognitect.transit :as transit]
            [plastic.main.frame :as frame]))

(defonce worker-script "/worker.js")
(defonce ^:dynamic worker nil)
(defonce ^:dynamic *last-job-id* 0)

(defn next-job-id! []
  (set! *last-job-id* (inc *last-job-id*))
  *last-job-id*)

(defn ^:export dispatch-message [data]
  (let [reader (transit/reader :json)
        command (aget data "command")
        id (aget data "id")]
    (condp = command
      "dispatch" (dispatch-args id (transit/read reader (aget data "args"))))))

(defn process-message [message]
  (dispatch-message (.-data message)))

(defn spawn-workers [base-path]
  (if-not plastic.env.run-worker-on-main-thread
    (let [script-url (str base-path worker-script)
          new-worker (js/Worker. script-url)]
      (.addEventListener new-worker "message" process-message)
      (set! worker new-worker))))

(defn post-dispatch-message [args id]
  (let [writer (transit/writer :json)
        data (js-obj
               "id" id
               "command" "dispatch"
               "args" (transit/write writer args))]
    (if-not plastic.env.run-worker-on-main-thread
      (.postMessage worker data)
      (plastic.worker.servant.dispatch-message data))))

(defn ^:export dispatch-on-worker
  ([event] (dispatch-on-worker event nil))
  ([event continuation] (let [job-id (if continuation (next-job-id!) 0)]
                          (if continuation
                            (frame/register-job job-id continuation))
                          (post-dispatch-message event job-id))))