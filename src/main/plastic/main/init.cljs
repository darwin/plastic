(ns plastic.main.init
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.frame :refer [handle-event-and-report-exceptions] :refer-macros [worker-dispatch dispatch]]
            [reagent.core :as reagent]))

; -------------------------------------------------------------------------------------------------------------------

(defn init [context db [_state]]
  (worker-dispatch context [:init])
  db)

(defn job-done [context db [job-id undo-summary]]
  db
  #_(let [{:keys [frame]} context
          job (jobs/get-job context job-id)
          coallesced-db (reagent/atom db)]
      (doseq [event (jobs/events job)]                                                                                ; replay all buffered job events...
        (handle-event-and-report-exceptions frame coallesced-db event))
      (jobs/unregister-job context job-id)
      (or
        (when-let [result-db ((jobs/continuation job) @coallesced-db undo-summary)]
          (if-not (identical? db result-db)
            (doseq [{:keys [editor-id description]} undo-summary]
              (let [old-editor (get-in db [:editors editor-id])]
                (dispatch context [:store-editor-undo-snapshot editor-id description old-editor]))))
          result-db)
        db)))