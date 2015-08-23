(ns plastic.main.undo
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [worker-dispatch worker-dispatch-args]])
  (:require [plastic.main.frame :refer [register-handler]]
            [plastic.undo :as undo]))

(defn undo [db [editor-id]]
  (or
    (if (undo/can-undo? db editor-id)
      (worker-dispatch-args [:undo editor-id]
        (fn [db-after-worker-undo]
          (undo/do-undo db-after-worker-undo [editor-id]))))
    db))

(defn redo [db [editor-id]]
  (or
    (if (undo/can-redo? db editor-id)
      (worker-dispatch-args [:redo editor-id]
        (fn [db-after-worker-redo]
          (undo/do-redo db-after-worker-redo [editor-id]))))
    db))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :store-editor-undo-snapshot undo/push-undo)
(register-handler :store-editor-redo-snapshot undo/push-redo)

(register-handler :undo undo)
(register-handler :redo redo)
