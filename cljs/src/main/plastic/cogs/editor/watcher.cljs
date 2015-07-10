(ns plastic.cogs.editor.watcher
  (:require [plastic.frame.core :refer [subscribe]]
            [clojure.data :as data]
            [plastic.util.helpers :as helpers])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react!]]))

(defonce ^:dynamic prev nil)

(def editors-subscription (subscribe [:editors]))

; for debugging only - this may be slow
#_(react!
  (when-let [editors @editors-subscription]
    (log "editors changed:" (data/diff prev editors) editors)
    (set! prev editors)))