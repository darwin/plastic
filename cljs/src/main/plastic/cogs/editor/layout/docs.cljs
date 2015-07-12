(ns plastic.cogs.editor.layout.docs
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.node :as node]
            [plastic.cogs.editor.layout.utils :as layout-utils]))

(defn doc-item [nodes [id info]]
  (let [parser-node (get nodes id)
        text (node/string parser-node)
        {:keys [editing?]} info
        {:keys [id]} parser-node]
    (merge {:id          id
            :tag         :token
            :doc?        true
            :selectable? true
            :line        -1
            :text        (layout-utils/prepare-string-for-display text)}
      (if editing? {:editing? true}))))

(defn build-docs-render-tree [analysis nodes]
  (let [docs (filter (fn [[_node info]] (:def-doc? info)) analysis)]
    {:tag      :docs
     :children (map (partial doc-item nodes) docs)}))