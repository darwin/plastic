(ns plastic.cogs.editor.layout.headers
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.node :as node]))

(defn header-item [[_node info]]
  (let [name-node (:def-name-node info)
        name (if name-node (node/string name-node))]
    (if name
      {:id   (:id name-node)
       :name name})))

(defn build-headers-render-tree [loc]
  nil
  #_(let [headers (filter (fn [[_node info]] (:def? info)) analysis)]
    {:tag      :headers
     :children (map header-item headers)}))