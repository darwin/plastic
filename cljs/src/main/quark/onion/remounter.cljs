(ns quark.onion.remounter
  (:require [quark.cogs.editor.renderer :refer [mount-editor]]
            [quark.onion.inface :refer [ids->views]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [dispatch react!]]))

; for figwheel
(defn ^:export remount-editors []
  (doseq [[editor-id atom-view] @ids->views]
    (mount-editor (.-element atom-view) editor-id)))