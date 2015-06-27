(ns quark.cogs.editors.watcher
  (:require [quark.frame.core :refer [subscribe]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react!]]))

(def editors-substription (subscribe [:editors]))

(react!
  (if-let [editors @editors-substription]
    (log "editors changed:" editors)))