(ns plastic.main.frame
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [plastic.main :refer [dispatch-args]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [reagent.core :as reagent]
            [plastic.main.db :refer [db]]
            [plastic.main.frame.jobs :as jobs]
            [re-frame.middleware :as middleware]
            [re-frame.frame :as frame]
            [re-frame.scaffold :as scaffold]))

(defonce ^:private event-chan (chan))
(defonce frame (atom (frame/make-frame)))

(defn current-job-desc
  ([] (current-job-desc plastic.env.*current-main-job-id*))
  ([id] (if-not (zero? id) (str "(job " id ")") "")))

(def bench? (or plastic.env.bench-processing plastic.env.bench-main-processing))

(defn timing [handler]
  (fn timing-handler [db v]
    (measure-time bench? "PROCESS" [v (current-job-desc)]
      (handler db v))))

(def register-job jobs/register-job)

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

(defn handle-event-and-report-exceptions [frame-atom db event]
  (try
    (frame/process-event-on-atom! @frame-atom db event)
    (catch :default e
      (error (.-stack e)))))

(defn job-done [db [job-id undo-summary]]
  (let [job (jobs/get-job job-id)
        coallesced-db (reagent/atom db)]
    (doseq [event (jobs/events job)]                                                                                  ; replay all buffered job events...
      (handle-event-and-report-exceptions frame coallesced-db event))
    (jobs/unregister-job job-id)
    (or
      (when-let [result-db ((jobs/continuation job) @coallesced-db)]
        (if-not (identical? db result-db)
          (doseq [{:keys [editor-id description]} undo-summary]
            (let [old-editor (get-in db [:editors editor-id])]
              (dispatch-args 0 [:store-editor-undo-snapshot editor-id description old-editor]))))
        result-db)
      db)))

(register-handler :worker-job-done job-done)

(defn handle-event [frame db job-id event]
  (binding [plastic.env/*current-thread* "MAIN"
            plastic.env/*current-main-job-id* job-id
            plastic.env/*current-main-event* event]
    (if (zero? job-id)
      (handle-event-and-report-exceptions frame db event)
      (jobs/buffer-job-event job-id event))))

(defn main-loop []
  (go-loop []
    (let [[job-id event] (<! event-chan)]
      (handle-event frame db job-id event))
    (recur)))

(defn ^:export dispatch [job-id event]
  (put! event-chan [job-id event])
  nil)

(defn ^:export dispatch-sync [job-id event]
  (assert (nil? plastic.env/*current-main-event*) "cannot call dispatch-sync during processing another dispatch")
  (handle-event frame db job-id event)
  nil)

