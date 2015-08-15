(ns plastic.main.editor.watcher
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react!]])
  (:require [plastic.main.frame :refer [subscribe]]
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