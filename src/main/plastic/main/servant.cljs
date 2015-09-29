(ns plastic.main.servant
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [plastic.main :refer [react! dispatch dispatch-args]])
  (:require [cognitect.transit :as transit]
            [plastic.main.frame :as frame]))

(defonce worker-script "/worker.js")
(defonce ^:dynamic worker nil)
(defonce ^:dynamic *last-job-id* 0)

(defonce reader (transit/reader :json))
(defonce writer (transit/writer :json))
(defonce benchmark? (or plastic.env.bench-transmit plastic.env.bench-main-transmit))

; -------------------------------------------------------------------------------------------------------------------

(defn next-job-id! []
  (set! *last-job-id* (inc *last-job-id*))
  *last-job-id*)

(declare process-message)

(defn spawn-workers [base-path]
  (if-not plastic.env.run-worker-on-main-thread
    (let [script-url (str base-path worker-script)
          new-worker (js/Worker. script-url)]
      (.addEventListener new-worker "message" #(process-message (.-data %)))
      (set! worker new-worker))))

(defn post-message [args id]
  (let [data (js-obj
               "id" id
               "command" "dispatch"
               "args" (measure-time benchmark? "TRANSMIT" ["encode args"] (transit/write writer args)))]
    (if-not plastic.env.run-worker-on-main-thread
      (.postMessage worker data)
      (plastic.worker.servant.process-message data))))

; -------------------------------------------------------------------------------------------------------------------

(defn ^:export process-message [data]
  (let [command (aget data "command")
        id (aget data "id")
        args (aget data "args")
        event (measure-time benchmark? "TRANSMIT" ["decode args"] (transit/read reader args))]
    (condp = command
      "dispatch" (dispatch-args id event))))

(defn ^:export dispatch-on-worker
  ([event] (dispatch-on-worker event nil))
  ([event continuation] (let [job-id (next-job-id!)]                                                                  ; TODO: this could be later relaxed, dispatchers would opt-in into job effects coallescing
                          (frame/register-job job-id (or continuation identity))
                          (post-message event job-id))))
