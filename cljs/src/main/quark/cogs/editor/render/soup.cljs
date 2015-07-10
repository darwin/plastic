(ns quark.cogs.editor.render.soup
  (:require [quark.cogs.editor.render.code :refer [code-token-component]]
            [quark.cogs.editor.render.utils :refer [classv]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn form-soup-overlay-component []
  (fn [soup]
    (log "R! soup-overlay" soup)
    [:div.form-overlay.form-soup-overlay
     (for [token soup]
       ^{:key (:id token)}
       [code-token-component token])]))