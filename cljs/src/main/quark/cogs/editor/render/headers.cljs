(ns quark.cogs.editor.render.headers
  (:require [quark.cogs.editor.render.utils :refer [raw-html wrap-specials id! classv]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [reagent.ratom :refer [reaction]]))

(defn header-component [doc-info]
  (let [{:keys [name]} doc-info]
    ^{:key (id!)}
    [:div.header
     (if name [:div.name
               [:div name]])]))

(defn headers-component [doc-info-list]
  [:div.headers-group
   (for [doc-info doc-info-list]
     (header-component doc-info))])

(defn headers-wrapper-component [form]
  [:div.headers-wrapper
   [headers-component (:headers form)]])