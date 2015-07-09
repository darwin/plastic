(ns quark.cogs.editor.watcher
  (:require [quark.frame.core :refer [subscribe]]
            [clojure.data :as data]
            [quark.util.helpers :as helpers])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react!]]))

(defonce ^:dynamic prev nil)

(def editors-subscription (subscribe [:editors]))

; for debugging only - this may be slow
#_(react!
  (when-let [editors @editors-subscription]
    (log "editors changed:" (data/diff prev editors) editors)
    (set! prev editors)))