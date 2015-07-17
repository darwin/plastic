(ns plastic.cogs.editor.render.headers
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.render.utils :refer [wrap-specials classv]]))

(defn header-component [header-info]
  (let [{:keys [name id]} header-info]
    ^{:key id}
    [:div.header
     (if name [:div.name [:div name]])]))

(defn headers-wrapper-component []
  (fn [_editor-id _form-id node]
    [:div.headers-group
     (for [header-info (:children node)]
       [header-component header-info])]))