(ns quark.cogs.editor.watcher
  (:require [quark.frame.core :refer [subscribe]]
            [clojure.data :as data])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react!]]))

(defonce ^:dynamic prev nil)

(def editors-subscription (subscribe [:editors]))

(react!
  (when-let [editors @editors-subscription]
    (log "editors changed:" (data/diff prev editors) editors)
    (set! prev editors)))