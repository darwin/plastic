(ns plastic.worker.frame
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [plastic.worker :refer [main-dispatch]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [plastic.worker.frame.counters :as counters]
            [plastic.worker.db :refer [db]]
            [plastic.worker.frame.subs :as subs]
            [plastic.worker.frame.middleware :refer [pure trim-v]]
            [plastic.worker.frame.router :refer [event-chan purge-chan]]
            [plastic.worker.frame.handlers :refer [handle register-base]]
            [cljs.core.async :refer [chan put! <!]]))

(def ^:dynamic *current-job-id* nil)

(defn timing [handler]
  (fn timing-handler [db v]
    (measure-time (or plastic.env.bench-processing plastic.env.bench-main-processing) "PROCESS" [v (str "#" *current-job-id*)]
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

(def subscribe (partial subs/subscribe db))
(def register-sub subs/register)

(defn handle-event-and-silently-swallow-exceptions [db event]
  (try
    (handle db event)
    (catch :default _)))

(defn worker-loop [db]
  (go-loop []
    (let [[job-id event] (<! event-chan)]
      (binding [*current-job-id* job-id
                plastic.env/*current-thread* "WORK"]
        (handle-event-and-silently-swallow-exceptions db event))
      (if-not (zero? job-id)                                ; jobs with id 0 are without continuation
        (counters/update-counter-for-job job-id))
      (recur))))

(defn ^:export dispatch [job-id event]
  (if-not (zero? job-id)                                    ; jobs with id 0 are without continuation
    (counters/inc-counter-for-job job-id))
  (put! event-chan [job-id event])
  nil)