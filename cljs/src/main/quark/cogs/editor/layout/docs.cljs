(ns quark.cogs.editor.layout.docs
  (:require [rewrite-clj.node :as node]
            [quark.cogs.editor.utils :refer [prepare-string-for-display ancestor-count loc->path leaf-nodes make-zipper collect-all-right]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn doc-item [[_node info]]
  (let [doc-node (:def-doc-node info)
        doc (if doc-node (node/string doc-node))]
    (if doc {:doc (prepare-string-for-display doc)})))

(defn build-docs-render-info [analysis _node]
  (let [docs (filter (fn [[_node info]] (:def? info)) analysis)]
    (map doc-item docs)))