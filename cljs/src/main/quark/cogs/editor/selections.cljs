(ns quark.cogs.editor.selections
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [quark.cogs.editor.selection.model :as model])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]))

; editor's :selections is a map of form-ids to sets of selected node-ids
; also has key :focused-form-id pointing to currently focused form

(defn apply-move-selection [editor movement]
  (let [selections (:selections editor)
        focused-form-id (:focused-form-id selections)
        find-form-info (fn [id] (some #(if (= (:id %) id) %) (get-in editor [:render-state :forms])))
        form-info (find-form-info focused-form-id)]
    (if form-info
      (let [cursel (get selections focused-form-id)
            result (model/op movement cursel form-info)]
        (if result
          (assoc-in editor [:selections focused-form-id] result)
          editor)))))

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
    (assoc-in editor [:selections] {:focused-form-id last-focused-form-id})))

(defn clear-all-selections [editors [editor-id]]
  (let [last-focused-form-id (get-in editors [editor-id :selections :focused-form-id])]
    (assoc-in editors [editor-id :selections] {:focused-form-id last-focused-form-id})))

(defn select [editors [editor-id form-id selections]]
  (assoc-in editors [editor-id :selections] {:focused-form-id form-id
                                             form-id          selections}))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-clear-all-selections paths/editors-path clear-all-selections)
(register-handler :editor-select paths/editors-path select)
