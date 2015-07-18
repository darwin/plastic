(ns plastic.cogs.editor.selections
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]]
                   [plastic.macros.common :refer [*->]])
  (:require [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.schema.paths :as paths]
            [plastic.cogs.editor.ops.editing :as editing]
            [plastic.cogs.editor.model :as editor]))

; ----------------------------------------------------------------------------------------------------------------

(defn clear-selection-in-editor [editor]
  (-> editor
    (editing/stop-editing)
    (editor/set-selection #{})))

(defn clear-selection [editors [editor-selector]]
  (editor/apply-to-specified-editors clear-selection-in-editor editors editor-selector))

(defn set-selection-in-editor [selection editor]
  (-> editor
    (editing/stop-editing)
    (editor/set-selection selection)))

(defn set-selection [editors [editor-selector selection]]
  (editor/apply-to-specified-editors (partial set-selection-in-editor selection) editors editor-selector))

(defn toggle-selection-in-editor [selection editor]
  (-> editor
    (editing/stop-editing)
    (editor/toggle-selection selection)))

(defn toggle-selection [editors [editor-selector selection]]
  (editor/apply-to-specified-editors (partial toggle-selection-in-editor selection) editors editor-selector))

(defn focus-form-in-editor [form-id editor]
  (-> editor
    (editor/set-focused-form-id form-id)))

(defn focus-form [editors [editor-selector form-id]]
  (editor/apply-to-specified-editors (partial focus-form-in-editor form-id) editors editor-selector))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-clear-selection paths/editors-path clear-selection)
(register-handler :editor-set-selection paths/editors-path set-selection)
(register-handler :editor-toggle-selection paths/editors-path toggle-selection)
(register-handler :editor-focus-form paths/editors-path focus-form)