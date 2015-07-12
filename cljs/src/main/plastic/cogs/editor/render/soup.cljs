(ns plastic.cogs.editor.render.soup
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.render.code :refer [code-token-component]]
            [plastic.cogs.editor.render.utils :refer [classv]]))

(defn form-soup-overlay-component []
  (fn [soup]
    [:div.form-overlay.form-soup-overlay
     (for [token soup]
       ^{:key (:id token)}
       [code-token-component token])]))