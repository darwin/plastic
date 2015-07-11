(ns plastic.cogs.editor.layout.headers
  (:require [rewrite-clj.node :as node]
            [plastic.cogs.editor.parser.utils :refer [next-node-id!]]
            [plastic.cogs.editor.layout.utils :refer [ancestor-count loc->path leaf-nodes make-zipper collect-all-right]])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

(defn header-item [[_node info]]
  (let [name-node (:def-name-node info)
        name (if name-node (node/string name-node))]
    (if name
      {:id   (:id name-node)
       :name name})))

(defn build-headers-render-tree [analysis _node]
  (let [headers (filter (fn [[_node info]] (:def? info)) analysis)]
    {:tag      :headers
     :id       (next-node-id!)
     :children (map header-item headers)}))