(ns plastic.cogs.editor.watcher
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react!]])
  (:require [plastic.frame.core :refer [subscribe]]
            [clojure.data :as data]))

(defonce ^:dynamic prev nil)

(def editors-subscription (subscribe [:editors]))

(comment

  ; for debugging only - this may be slow
  (react!
    (when-let [editors @editors-subscription]
      (log "editors changed:" (data/diff prev editors) editors)
      (set! prev editors)))

  )