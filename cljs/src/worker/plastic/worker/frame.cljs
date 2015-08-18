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
(defonce frame (atom (frame/make-frame)))

(defn current-job-desc
  ([] (current-job-desc *current-job-id*))
  ([id] (if-not (zero? id) (str "(job " id ")") "")))

(defn timing [handler]
  (fn timing-handler [db v]
    (measure-time (or plastic.env.bench-processing plastic.env.bench-main-processing) "PROCESS" [v (current-job-desc)]
      (handler db v))))

(def path (middleware/path frame))

(def common-middleware [timing (middleware/trim-v frame)])

(defn register-handler
  ([event-id handler-fn] (register-handler event-id nil handler-fn))
  ([event-id middleware handler-fn] (scaffold/register-base frame event-id [common-middleware middleware] handler-fn)))

(defn register-sub [subscription-id handler-fn]
  (swap! frame #(frame/register-subscription-handler % subscription-id handler-fn)))

(defn subscribe [subscription-spec]
  (scaffold/legacy-subscribe frame db subscription-spec))

(defn handle-event-and-report-exceptions [frame-atom db job-id event]
  (binding [*current-job-id* job-id]
    (try
      (frame/process-event-on-atom @frame-atom db event)
      (catch :default e
        (error e (.-stack e))))
    (if-not (zero? job-id)                                  ; jobs with id 0 are without continuation
      (counters/update-counter-for-job job-id))))

(defn ^:export dispatch [job-id event]
  (if-not (zero? job-id)                                    ; jobs with id 0 are without continuation
    (counters/inc-counter-for-job job-id))
  (put! event-chan [job-id event])
  nil)

(defn worker-loop []
  (binding [plastic.env/*current-thread* "WORK"]
    (go-loop []
      (let [[job-id event] (<! event-chan)]
        (handle-event-and-report-exceptions frame db job-id event))
      (recur))))
