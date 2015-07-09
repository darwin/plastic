(ns quark.onion.remounter
  (:require [quark.onion.inface :refer [ids->views find-mount-point]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [dispatch react!]]))

; for figwheel
(defn ^:export remount-editors []
  (doseq [[editor-id atom-view] @ids->views]
    (dispatch :mount-editor editor-id (find-mount-point (.-element atom-view)))))