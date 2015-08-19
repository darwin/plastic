(ns plastic.main.editor.highlight
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.frame :refer [register-handler]]
            [plastic.main.paths :as paths]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.ops.editing :as editing]))

; ----------------------------------------------------------------------------------------------------------------------

(defn update-highlight [editors [selector]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (log "update highlight" editor))))
; ----------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-update-highlight paths/editors-path update-highlight)
