(ns plastic.worker.frame
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [plastic.worker :refer [main-dispatch main-dispatch-args dispatch-args]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [plastic.worker.frame.counters :as counters]
            [cljs.core.async :refer [chan put! <!]]
            [plastic.worker.db :refer [db]]
            [re-frame.middleware :as middleware]
            [re-frame.scaffold :as scaffold]
            [re-frame.frame :as frame]
            [re-frame.utils :as utils]
            [plastic.worker.frame.undo :as undo]))

(defonce ^:private event-chan (chan))
(defonce frame (atom (frame/make-frame)))

(defn current-job-desc
  ([] (current-job-desc plastic.env.*current-worker-job-id*))
  ([id] (if-not (zero? id) (str "(job " id ")") "")))

(def bench? (or plastic.env.bench-processing plastic.env.bench-main-processing))

(defn timing [handler]
  (fn timing-handler [db v]
    (measure-time bench? "PROCESS" [v (current-job-desc)]
      (handler db v))))

(def path (middleware/path frame))

(def common-middleware [timing (middleware/trim-v frame)])

(defn register-handler
  ([event-id handler-fn]
   (register-handler event-id nil handler-fn))
  ([event-id middleware handler-fn]
   (scaffold/register-base frame event-id [common-middleware middleware] handler-fn)))

(defn register-sub [subscription-id handler-fn]
  (swap! frame #(frame/register-subscription-handler % subscription-id handler-fn)))

(defn subscribe [subscription-spec]
  (scaffold/legacy-subscribe frame db subscription-spec))

(defn handle-event-and-report-exceptions [frame-atom db-atom job-id event]
  (binding [plastic.env/*current-thread* "WORK"
            plastic.env/*current-worker-job-id* job-id
            plastic.env/*current-worker-event* event]
    (let [old-db @db-atom]
      (try
        (frame/process-event-on-atom! @frame-atom db-atom event)
        (catch :default e
          (error e (.-stack e))))
      (if-not (zero? job-id)                                                                                          ; jobs with id 0 are without continuation
        (if (counters/update-counter-for-job job-id)
          (let [undo-summary (undo/vacuum-undo-summary)]
            (main-dispatch-args 0 [:worker-job-done job-id undo-summary])
            (doseq [{:keys [editor-id description]} undo-summary]
              (let [old-editor (get-in old-db [:editors editor-id])]
                (dispatch-args 0 [:store-editor-undo-snapshot editor-id description old-editor])))))))))

(defn ^:export dispatch [job-id event]
  (if-not (zero? job-id)                                                                                              ; jobs with id 0 are without continuation
    (counters/inc-counter-for-job job-id))
  (put! event-chan [job-id event])
  nil)

(defn worker-loop []
  (go-loop []
    (let [[job-id event] (<! event-chan)]
      (handle-event-and-report-exceptions frame db job-id event))
    (recur)))
