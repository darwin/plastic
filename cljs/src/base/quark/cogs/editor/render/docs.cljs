(ns quark.cogs.editor.render.docs
  (:require [quark.cogs.editor.render.utils :refer [raw-html wrap-specials id! classv]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [reagent.ratom :refer [reaction]]))

(defn doc-component [doc-info]
  (let [{:keys [name doc]} doc-info]
    ^{:key (id!)}
    [:div.doc
     (if name [:div.name name])
     (if doc [:div.docstring (raw-html (wrap-specials doc))])]))

(defn docs-component [doc-info-list]
  [:div.docs-group
   (for [doc-info doc-info-list]
     (doc-component doc-info))])

(defn docs-wrapper-component [form]
  [:div.docs
   [docs-component (:docs-tree form)]])