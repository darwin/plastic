(ns plastic.cogs.editor.selections
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]])
  (:require [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.schema.paths :as paths]
            [plastic.cogs.editor.selection.model :as model]
            [plastic.cogs.editor.layout.utils :refer [apply-to-selected-editors]]
            [plastic.cogs.editor.editing :as editing]))

; editor's :selections is a map of form-ids to sets of selected node-ids
; also has key :focused-form-id pointing to currently focused form

(defn apply-movements [editor movements]
  (if-let [movement (first movements)]
    (if-let [result (model/op movement editor)]
      result
      (recur editor (rest movements)))))

(defn apply-move-selection [editor & movements]
  (if-let [result (apply-movements editor movements)]
    result
    editor))

; ---------------------------
; spatial movement

(defn spatial-up [editor]
  (apply-move-selection editor :spatial-up :move-prev-form))

(defn spatial-down [editor]
  (apply-move-selection editor :spatial-down :move-next-form))

(defn spatial-left [editor]
  (apply-move-selection editor :spatial-left))

(defn spatial-right [editor]
  (apply-move-selection editor :spatial-right))

; ---------------------------
; structural movement

(defn structural-up [editor]
  (apply-move-selection editor :structural-up))

(defn structural-down [editor]
  (apply-move-selection editor :structural-down))

(defn structural-left [editor]
  (apply-move-selection editor :structural-left))

(defn structural-right [editor]
  (apply-move-selection editor :structural-right))

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
