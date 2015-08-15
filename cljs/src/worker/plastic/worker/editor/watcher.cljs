(ns plastic.worker.editor.watcher
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [react!]])
  (:require [plastic.worker.frame.core :refer [subscribe]]
            [clojure.data :as data]))

(defonce ^:dynamic prev nil)

(comment
  (def editors-subscription (subscribe [:editors]))

  ; for debugging only - this may be slow
  (react!
    (when-let [editors @editors-subscription]
      (log "editors changed:" (data/diff prev editors) editors)
      (set! prev editors)))

  )