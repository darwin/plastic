(ns plastic.main.editor.render.docs
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main.render :refer [log-render]])
  (:require [plastic.main.editor.render.utils :refer [dangerously-set-html wrap-specials classv]]
            [plastic.main.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.main.editor.render.reusables :refer [raw-html-component]]
            [plastic.main.frame.core :refer [subscribe]]))

(defn doc-component [editor-id form-id node-id]
  (let [selected? (subscribe [:editor-selection-node editor-id node-id])
        edited? (subscribe [:editor-editing-node editor-id node-id])
        layout (subscribe [:editor-form-node-layout editor-id form-id node-id])]
    (fn [_editor-id _form-id node-id]
      (log-render "doc" node-id
        (let [{:keys [text id selectable?]} @layout]
          ^{:key id}
          [:div.doc
           [:div.docstring.token
            {:data-qnid id
             :class     (classv
                          (if (and (not @edited?) selectable?) "selectable")
                          (if (and (not @edited?) selectable? @selected?) "selected")
                          (if @edited? "editing"))}
            (if @edited?
              [inline-editor-component id text :string]
              [raw-html-component (str (wrap-specials text) " ")])]]))))) ; that added space is important, last newline could be ignored without it

(defn docs-group-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-form-node-layout editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (log-render "docs-group" node-id
        [:div.docs-group
         (for [doc-id (:children @layout)]
           ^{:key doc-id} [doc-component editor-id form-id doc-id])]))))