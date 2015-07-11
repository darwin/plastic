(ns plastic.cogs.editor.layout.docs
  (:require [rewrite-clj.node :as node]
            [plastic.cogs.editor.parser.utils :refer [next-node-id!]]
            [plastic.cogs.editor.layout.utils :refer [prepare-string-for-display ancestor-count loc->path leaf-nodes make-zipper collect-all-right]])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

(defn doc-item [nodes [id info]]
  (let [parser-node (get nodes id)
        text (node/string parser-node)
        {:keys [editing?]} info
        {:keys [id]} parser-node]
    (merge {:id          id
            :tag         :token
            :selectable? true
            :line        -1
            :text        (prepare-string-for-display text)}
      (if editing? {:editing? true}))))

(defn build-docs-render-tree [analysis nodes]
  (let [docs (filter (fn [[_node info]] (:def-doc? info)) analysis)]
    {:tag      :docs
     :id       (next-node-id!)
     :children (map (partial doc-item nodes) docs)}))