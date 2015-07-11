(ns plastic.cogs.editor.selections
  (:require [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.schema.paths :as paths]
            [plastic.cogs.editor.selection.model :as model]
            [plastic.cogs.editor.layout.utils :refer [apply-to-selected-editors]]
            [plastic.cogs.editor.editing :as editing]
            [plastic.cogs.editor.model :as editor])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]]))

; editor's :selections is a map of form-ids to sets of selected node-ids
; also has key :focused-form-id pointing to currently focused form

(defn apply-movements [editor movements]
  (if-let [render-info (editor/get-focused-render-info editor)]
    (if-let [movement (first movements)]
      (if-let [result-selection (model/op movement (editor/get-focused-selection editor) render-info)]
        (editor/set-focused-selection editor result-selection)
        (recur editor (rest movements))))))

(defn apply-move-selection [editor & movements]
  (if-let [result (apply-movements editor movements)]
    result
    editor))

(defn move-up [editor]
  (apply-move-selection editor :move-up))

(defn move-down [editor]
  (apply-move-selection editor :move-down))

(defn move-left [editor]
  (apply-move-selection editor :move-left))

(defn move-right [editor]
  (apply-move-selection editor :move-right))

(defn level-up [editor]
  (apply-move-selection editor :level-up))

(defn level-down [editor]
  (apply-move-selection editor :level-down))

; ----------------------------------------------------------------------------------------------------------------

(defn clear-all-selections-in-editor [editor]
  (let [last-focused-form-id (get-in editor [:selections :focused-form-id])]
    (-> editor
      (editing/stop-editing)
      (assoc-in [:selections] {:focused-form-id last-focused-form-id}))))

(defn clear-all-selections [editors [editor-selector]]
  (apply-to-selected-editors clear-all-selections-in-editor editors editor-selector))

(defn select-in-editor [form-id selections editor]
  (-> editor
    (editing/stop-editing)
    (assoc-in [:selections] {:focused-form-id form-id
                             form-id          selections})))

(defn select [editors [editor-selector form-id selections]]
  (apply-to-selected-editors (partial select-in-editor form-id selections) editors editor-selector))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-clear-all-selections paths/editors-path clear-all-selections)
(register-handler :editor-select paths/editors-path select)
