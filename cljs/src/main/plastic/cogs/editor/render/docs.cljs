(ns plastic.cogs.editor.render.docs
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.render.utils :refer [dangerously-set-html wrap-specials classv]]
            [plastic.cogs.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.cogs.editor.render.reusables :refer [raw-html-component]]
            [plastic.frame.core :refer [subscribe]]))

(defn doc-component [editor-id form-id node-id]
  (let [selected? (subscribe [:editor-selection-node editor-id node-id])
        edited? (subscribe [:editor-editing-node editor-id node-id])]
    (fn [doc-info]
      (let [{:keys [text id selectable?]} doc-info]
        ^{:key id}
        [:div.doc
         [:div.docstring.token
          {:data-qnid id
           :class     (classv
                        (if (and (not @edited?) selectable?) "selectable")
                        (if (and (not @edited?) selectable? @selected?) "selected")
                        (if @edited? "editing"))}
          (if @edited?
            [inline-editor-component id text :doc]
            [raw-html-component (str (wrap-specials text) " ")])]])))) ; that added space is important, last newline could be ignored without it

(defn docs-component []
  (fn [editor-id form-id docs-render-info]
    [:div.docs-group
     (for [doc-info (:children docs-render-info)]
       (let [id (:id doc-info)]
         ^{:key id}
         [(doc-component editor-id form-id id) doc-info]))]))