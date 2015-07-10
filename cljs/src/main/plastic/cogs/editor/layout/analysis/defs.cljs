(ns plastic.cogs.editor.layout.analysis.defs
  (:require [plastic.util.helpers :as helpers]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.stringz :refer [StringNode]]
            [rewrite-clj.node.token :refer [TokenNode]]
            [plastic.cogs.editor.layout.utils :refer [unwrap-metas node-walker noop]])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

(defn first-child-sexpr [node]
  (first (node/child-sexprs node)))

(defn essential-nodes [nodes]
  (filter #(not (or (node/whitespace? %) (node/comment? %))) nodes))

(defn is-def? [node]
  {:pre  [(= (node/tag node) :list)]}
  (re-find #"^def" (str (first-child-sexpr node))))

(defn string-node? [node]
  (instance? StringNode node))

(defn symbol-node? [node]
  (instance? TokenNode node))

(defn extract-sym-doc [node]
  (let [children (essential-nodes (unwrap-metas (node/children node)))
        first-string-node (first (filter string-node? children))
        first-symbol-node (first (rest (filter symbol-node? children)))]
    [(if first-symbol-node
       {(:id first-symbol-node) {:def-name? true}})
     (if first-string-node
       {(:id first-string-node) {:def-doc? true :selectable true}})
     {(:id node) {:def?          true
            :def-name-node first-symbol-node
            :def-doc-node  first-string-node}}]))

(defn def-info [node]
  (when (is-def? node)
    (extract-sym-doc node)))

(defn list-children [node]
  (filter #(= (node/tag %) :list) (node/children node)))

(defn analyze-defs [node info]
  (let [walker (node-walker def-info noop helpers/deep-merge list-children)]
    (helpers/deep-merge info (walker node))))