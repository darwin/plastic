(ns plastic.cogs.editor.selections
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]]
                   [plastic.macros.common :refer [*->]])
  (:require [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.schema.paths :as paths]
            [plastic.cogs.editor.layout.utils :refer [apply-to-selected-editors]]
            [plastic.cogs.editor.ops.editing :as editing]
            [plastic.cogs.editor.model :as editor]))

; ----------------------------------------------------------------------------------------------------------------

(defn clear-selection-in-editor [editor]
  (-> editor
    (editing/stop-editing)
    (editor/set-selection #{})))

(defn clear-selection [editors [editor-selector]]
  (apply-to-selected-editors clear-selection-in-editor editors editor-selector))

(defn select-in-editor [selection editor]
  (-> editor
    (editing/stop-editing)
    (editor/set-selection selection)))

(defn select [editors [editor-selector selection]]
  (apply-to-selected-editors (partial select-in-editor selection) editors editor-selector))

(defn focus-form-in-editor [form-id editor]
  (-> editor
    (editor/set-focused-form-id form-id)))

(defn focus-form [editors [editor-selector form-id]]
  (apply-to-selected-editors (partial focus-form-in-editor form-id) editors editor-selector))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-clear-selection paths/editors-path clear-selection)
(register-handler :editor-select paths/editors-path select)
(register-handler :editor-focus-form paths/editors-path focus-form)