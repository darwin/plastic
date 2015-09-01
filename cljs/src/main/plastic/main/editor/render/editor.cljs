(ns plastic.main.editor.render.editor
  (:require-macros [plastic.logging :refer [log info warn error group group-end log-render]]
                   [plastic.main :refer [react! dispatch]])
  (:require [plastic.main.frame :refer [subscribe]]
            [plastic.main.editor.render.form :refer [form-component]]
            [plastic.main.editor.render.utils :refer [classv]]))

(defn handle-editor-click [editor-id event]
  (.stopPropagation event)
  (dispatch :editor-clear-selection editor-id)
  (dispatch :editor-clear-cursor editor-id))

(defn editor-root-component [editor-id]
  (let [state (subscribe [:editor-render-state editor-id])
        selections-debug-visible (subscribe [:settings :selections-debug-visible])]
    (fn [editor-id]
      (log-render "editor-root" editor-id
        (let [{:keys [order]} @state]
          [:div.plastic-editor                                                                                        ; .editor class is taken by Atom
           {:data-peid editor-id
            :class     (classv (if @selections-debug-visible "debug-selections"))
            :on-click  (partial handle-editor-click editor-id)}
           [:table.form-table
            [:tbody
             (for [form-id order]
               ^{:key form-id} [form-component editor-id form-id])]]])))))
