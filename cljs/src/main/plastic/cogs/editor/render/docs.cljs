(ns plastic.cogs.editor.render.docs
  (:require [plastic.cogs.editor.render.utils :refer [dangerously-set-html wrap-specials classv]]
            [plastic.cogs.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.cogs.editor.render.reusables :refer [raw-html-component]])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

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
        [inline-editor-component text id]
        [raw-html-component (wrap-specials text)])]]))

(defn docs-component [doc-info-list]
  [:div.docs-group
   (for [doc-info doc-info-list]
     (doc-component doc-info))])

(defn docs-wrapper-component [form]
  [:div.docs-wrapper
   [docs-component (:docs form)]])