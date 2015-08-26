(ns plastic.main.editor.render.docs
  (:require-macros [plastic.logging :refer [log info warn error group group-end log-render]])
  (:require [plastic.main.editor.render.utils :refer [dangerously-set-html wrap-specials classv]]
            [plastic.main.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.main.editor.render.reusables :refer [raw-html-component]]
            [plastic.main.frame :refer [subscribe]]))

(defn doc-component [editor-id form-id node-id]
  (let [selected? (subscribe [:editor-selection-node editor-id node-id])
        cursor? (subscribe [:editor-cursor-node editor-id node-id])
        editing? (subscribe [:editor-editing-node editor-id node-id])
        layout (subscribe [:editor-layout-form-node editor-id form-id node-id])]
    (fn [_editor-id _form-id node-id]
      (log-render "doc" node-id
        (let [{:keys [text id selectable?]} @layout
              selected? @selected?
              editing? @editing?
              cursor? @cursor?]
          ^{:key id}
          [:div.doc
           [:div.docstring.token
            {:data-qnid id
             :class     (classv
                          (if (and (not editing?) selectable?) "selectable")
                          (if (and (not editing?) selectable? selected?) "selected")
                          (if cursor? "cursor")
                          (if editing? "editing"))}
            (if editing?
              [inline-editor-component id]
              [raw-html-component (str (wrap-specials text) " ")])]])))))                                             ; that added space is important, last newline could be ignored without it

(defn docs-group-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-layout-form-node editor-id form-id node-id])
        docs-visible (subscribe [:settings :docs-visible])]
    (fn [editor-id form-id node-id]
      (log-render "docs-group" node-id
        [:div.docs-group
         (if @docs-visible
           (for [doc-id (:children @layout)]
             ^{:key doc-id} [doc-component editor-id form-id doc-id]))]))))
