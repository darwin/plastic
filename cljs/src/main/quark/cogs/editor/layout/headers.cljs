(ns quark.cogs.editor.layout.headers
  (:require [rewrite-clj.node :as node]
            [quark.cogs.editor.utils :refer [ancestor-count loc->path leaf-nodes make-zipper collect-all-right]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn header-item [[_node info]]
  (let [name-node (:def-name-node info)
        name (if name-node (node/string name-node))]
    (if name
      {:id (:id name-node)
       :name name})))

(defn build-headers-render-info [analysis _node]
  (let [headers (filter (fn [[_node info]] (:def? info)) analysis)]
    (map header-item headers)))