(ns plastic.main.editor.watcher
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react!]])
  (:require [plastic.main.frame :refer [subscribe]]
            [clojure.data :as data]))

(defonce ^:dynamic prev nil)


(defn init []
  ; for debugging only - this may be slow
  #_(let [editors-subscription (subscribe [:editors])]
    (react!
      (when-let [editors @editors-subscription]
        (log "editors changed:" (data/diff prev editors) editors)
        (set! prev editors)))))

