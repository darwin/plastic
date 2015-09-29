(ns plastic.worker.editor.watcher
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.worker :refer [react!]])
  (:require [plastic.worker.frame :refer [subscribe]]
            [clojure.data :as data]))

(defonce ^:dynamic prev nil)

; note: reactions here are allowed to leak, used only for development

(defn init []
  ; for debugging only - this may be slow
  #_(let [editors-subscription (subscribe [:editors])]
      (react!
        (when-let [editors @editors-subscription]
          (log "editors changed:" (data/diff prev editors) editors)
          (set! prev editors)))))

(defn init-editor [editor-id]
  #_(if plastic.env.log-parse-tree
    (let [parse-tree-sub (subscribe [:editor-parse-tree editor-id])]
      (react!
        (when-let [parse-tree @parse-tree-sub]
          (fancy-log "PARSE-TREE" (node/string parse-tree)))))))
