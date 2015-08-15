(ns plastic.worker.frame
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [plastic.worker :refer [main-dispatch]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [plastic.env :as env]
            [plastic.worker.frame.core :as frame :refer [pure trim-v]]
            [plastic.worker.frame.router :refer [event-chan purge-chan]]
            [plastic.worker.frame.handlers :refer [handle register-base]]
            [cljs.core.async :refer [chan put! <!]]))

(def ^:dynamic *current-job-id* nil)
(def ^:dynamic *pending-jobs-counters* {})

(defn timing [handler]
  (fn timing-handler [db v]
    (measure-time (or env/bench-processing env/bench-main-processing) "PROCESS" [v (str "#" *current-job-id*)]
      (handler db v))))

(defn log-ex [handler]
  (fn log-ex-handler [db v]
    (try
      (handler db v)
      (catch js/Error e                                     ;  You don't need it any more IF YOU ARE USING CHROME 44. Chrome now seems to now produce good stack traces.
        (do
          (.error js/console (.-stack e))
          (throw e)))
      (catch :default e
        (do
          (.error js/console e)
          (throw e))))))

(defn register-handler
  ([id handler] (register-handler id nil handler))
  ([id middleware handler] (register-base id [pure log-ex timing trim-v middleware] handler)))

(def subscribe frame/subscribe)

(defn handle-event-and-silently-swallow-exceptions [event]
  (try
    (handle event)
    (catch :default _)))

(defn inc-counter-for-job [job-id]
  (set! *pending-jobs-counters* (update *pending-jobs-counters* job-id inc)))

(defn dec-counter-for-job [job-id]
  (set! *pending-jobs-counters* (update *pending-jobs-counters* job-id dec))
  (get *pending-jobs-counters* job-id))

(defn remove-counter-for-job [job-id]
  (set! *pending-jobs-counters* (dissoc *pending-jobs-counters* job-id)))

(defn update-counter-for-job [job-id]
  (when (zero? (dec-counter-for-job job-id))
    (remove-counter-for-job job-id)
    (main-dispatch :worker-job-done job-id)))

(defn worker-loop []
  (go-loop []
    (let [[job-id event] (<! event-chan)]
      (binding [*current-job-id* job-id
                plastic.env/*current-thread* "WORK"]
        (handle-event-and-silently-swallow-exceptions event))
      (if-not (zero? job-id)                                ; jobs with id 0 are without continuation
        (update-counter-for-job job-id))
      (recur))))

(defn dispatch [job-id event]
  (if-not (zero? job-id)                                    ; jobs with id 0 are without continuation
    (inc-counter-for-job job-id))
  (put! event-chan [job-id event])
  nil)