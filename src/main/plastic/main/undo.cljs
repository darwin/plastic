(ns plastic.main.undo
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.frame :refer [worker-dispatch]])
  (:require [plastic.undo :as undo]))

; -------------------------------------------------------------------------------------------------------------------

(defn undo [context db [editor-id]]
  (or
    (if (undo/can-undo? context db editor-id)
      (worker-dispatch context [:undo editor-id]
        (fn [db-after-worker-undo]
          (undo/do-undo context db-after-worker-undo [editor-id]))))
    db))

(defn redo [context db [editor-id]]
  (or
    (if (undo/can-redo? context db editor-id)
      (worker-dispatch context [:redo editor-id]
        (fn [db-after-worker-redo]
          (undo/do-redo context db-after-worker-redo [editor-id]))))
    db))
