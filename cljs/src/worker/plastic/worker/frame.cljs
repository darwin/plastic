(ns plastic.worker.frame
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [plastic.worker :refer [main-dispatch]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [plastic.worker.frame.counters :as counters]
            [cljs.core.async :refer [chan put! <!]]
            [plastic.worker.db :refer [db]]
            [re-frame.middleware :as middleware]
            [re-frame.scaffold :as scaffold]
            [re-frame.frame :as frame]
            [re-frame.utils :as utils]))

(defonce ^:dynamic *current-job-id* nil)
(defonce ^:private event-chan (chan))
(defonce worker-frame (atom (frame/make-frame)))

(defn timing [handler]
  (fn timing-handler [db v]
    (measure-time (or plastic.env.bench-processing plastic.env.bench-main-processing) "PROCESS" [v (str "#" *current-job-id*)]
      (handler db v))))

(def path (middleware/path worker-frame))

(def common-middleware [timing (middleware/trim-v worker-frame)])

(defn register-handler
  ([event-id handler-fn] (register-handler event-id nil handler-fn))
  ([event-id middleware handler-fn] (scaffold/register-base worker-frame event-id [common-middleware middleware] handler-fn)))

(defn register-sub [subscription-id handler-fn]
  (swap! worker-frame #(frame/register-subscription-handler % subscription-id handler-fn)))

(defn subscribe [subscription-spec]
  (scaffold/legacy-subscribe worker-frame db subscription-spec))

(defn handle-event-and-report-exceptions [frame-atom db event]
  (try
    (scaffold/transduce-event-by-resetting-atom frame-atom db event) ; let re-frame handle the event
    (catch :default e
      (error e (.-stack e)))))

(defn ^:export dispatch [job-id event]
  (if-not (zero? job-id)                                    ; jobs with id 0 are without continuation
    (counters/inc-counter-for-job job-id))
  (put! event-chan [job-id event])
  nil)

(defn worker-loop []
  (go-loop []
    (let [[job-id event] (<! event-chan)]
      (binding [*current-job-id* job-id
                plastic.env/*current-thread* "WORK"]
        (handle-event-and-report-exceptions worker-frame db event))
      (if-not (zero? job-id)                                ; jobs with id 0 are without continuation
        (counters/update-counter-for-job job-id))
      (recur))))
