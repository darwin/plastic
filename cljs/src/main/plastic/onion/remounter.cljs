(ns plastic.onion.remounter
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main.glue :refer [dispatch react!]])
  (:require [plastic.main.frame]
            [plastic.onion.inface :refer [ids->views find-mount-point]]))

; for figwheel
(defn ^:export remount-editors []
  (doseq [[editor-id atom-view] @ids->views]
    (dispatch :mount-editor editor-id (find-mount-point (.-element atom-view)))))