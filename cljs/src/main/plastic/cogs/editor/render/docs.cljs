(ns plastic.cogs.editor.render.docs
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.render.utils :refer [dangerously-set-html wrap-specials classv]]
            [plastic.cogs.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.cogs.editor.render.reusables :refer [raw-html-component]]
            [plastic.frame.core :refer [subscribe]]))

(defn doc-component [doc-info]
  (let [{:keys [text id editing? selectable?]} doc-info]
    ^{:key id}
    [:div.doc
     [:div.docstring.selectable
      {:data-qnid id
       :class     (classv
                    (if selectable? "selectable")
                    (if editing? "editing"))}
      (if editing?
        [inline-editor-component id text :doc]
        [raw-html-component (str (wrap-specials text) " ")])]])) ; that added space is important, last newline could be ignored without it

(defn docs-component []
  (fn [docs-render-info]
    [:div.docs-group
     (for [doc-info (:children docs-render-info)]
       (doc-component doc-info))]))