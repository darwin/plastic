(ns plastic.main.editor.selection
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.frame :refer [register-handler]]
            [plastic.main.paths :as paths]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.ops.editing :as editing]))

; -------------------------------------------------------------------------------------------------------------------

(defn clear-selection [editors [selector]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (editing/stop-editing editor #(editor/set-selection % #{})))))

(defn set-selection [editors [selector selection]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (editing/stop-editing editor #(editor/set-selection % selection)))))

(defn toggle-selection [editors [selector selection]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (editing/stop-editing editor #(editor/toggle-selection % selection)))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-clear-selection paths/editors-path clear-selection)
(register-handler :editor-set-selection paths/editors-path set-selection)
(register-handler :editor-toggle-selection paths/editors-path toggle-selection)