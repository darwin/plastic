(ns quark.cogs.editor.render.docs
  (:require [quark.cogs.editor.render.utils :refer [raw-html wrap-specials classv]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn doc-component [doc-info]
  (let [{:keys [doc id]} doc-info]
    ^{:key id}
    [:div.doc
     (if doc [:div.docstring.selectable
              (merge
                {:data-qid id}
                (raw-html (wrap-specials doc)))])]))

(defn docs-component [doc-info-list]
  [:div.docs-group
   (for [doc-info doc-info-list]
     (doc-component doc-info))])

(defn docs-wrapper-component [form]
  [:div.docs-wrapper
   [docs-component (:docs form)]])