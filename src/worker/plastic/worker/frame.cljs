(ns plastic.worker.frame
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]])
  (:require [re-frame.frame :refer [make-frame]]
            [plastic.frame :refer [start-loop init-frame]]
            [plastic.util.booking :refer [make-booking]]
            [plastic.worker.frame.db :refer [make-db]]
            [plastic.worker.frame.handlers :refer [register-handlers]]
            [plastic.worker.frame.subs :refer [register-subs]]
            [plastic.worker.servant :refer [init-servant]]))

; -------------------------------------------------------------------------------------------------------------------

;(defn handle-event [context job-id event]
;  {:pre [(not globals/*current-event*)]}
;  (binding [globals/*current-thread* (get-thread-label context)
;            globals/*current-event* event]
;    (let [{:keys [frame db]} context]
;      (set-initial-db-for-job! context job-id @db)                                                                       ; if first event of job, store current db as initial-db for job
;      (handle-event-and-report-exceptions frame db event)
;      (if-let [initial-db (update-counter-for-job-and-pop-initial-db-if-finished! context job-id)]
;        (let [undo-summary (undo/vacuum-undo-summary)]
;          (main-dispatch-args context 0 [:worker-job-done job-id undo-summary])
;          (doseq [{:keys [editor-id description]} undo-summary]
;            (let [old-editor (get-in initial-db [:editors editor-id])]
;              (dispatch-args context 0 [:store-editor-undo-snapshot editor-id description old-editor]))))))))

; -------------------------------------------------------------------------------------------------------------------

(defn start [context]
  (-> context
    (assoc :main-context (volatile! nil))
    (assoc :db (make-db context))
    (assoc :aux (make-booking))
    (init-frame)
    (init-servant)
    (register-handlers)
    (register-subs)
    (start-loop)))

(defn stop [context]
  (warn "not implemented"))