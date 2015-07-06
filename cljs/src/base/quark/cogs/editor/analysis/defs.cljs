(ns quark.cogs.editor.analysis.defs
  (:require [quark.util.helpers :as helpers]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.stringz :refer [StringNode]]
            [rewrite-clj.node.token :refer [TokenNode]]
            [quark.cogs.editor.utils :refer [essential-nodes node-children-unwrap-metas essential-children node-walker]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn first-child-sexpr [node]
  (first (node/child-sexprs node)))

(defn is-def? [node]
  (and
    (= (node/tag node) :list)
    (re-find #"^def" (str (first-child-sexpr node)))))

(defn string-node? [node]
  (instance? StringNode node))

(defn symbol-node? [node]
  (instance? TokenNode node))

(defn extract-sym-doc [node]
  (let [children (essential-nodes (node-children-unwrap-metas node))
        first-string-node (first (filter string-node? children))
        first-symbol-node (first (rest (filter symbol-node? children)))]
    [(if first-symbol-node
       {first-symbol-node {:def-name? true}})
     (if first-string-node
       {first-string-node {:def-doc? true}})
     {node {:def?          true
            :def-name-node first-symbol-node
            :def-doc-node  first-string-node}}]))

(defn def-info [node]
  (when (is-def? node)
    (extract-sym-doc node)))

(defn analyze-defs [node info]
  (let [walker (node-walker def-info (fn [] []) helpers/deep-merge essential-children)]
    (helpers/deep-merge info (walker node))))
