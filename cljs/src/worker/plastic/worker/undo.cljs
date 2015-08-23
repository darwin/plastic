(ns plastic.worker.undo
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.frame :refer [register-handler]]
            [plastic.undo :as undo]))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :store-editor-undo-snapshot undo/push-undo)
(register-handler :store-editor-redo-snapshot undo/push-redo)

(register-handler :undo undo/do-undo)
(register-handler :redo undo/do-redo)
