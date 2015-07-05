(ns quark.cogs.editor.analysis.defs
  (:require [cljs.core.async :refer [<! timeout]]
            [quark.frame.core :refer [subscribe register-handler]]
            [quark.util.helpers :as helpers]
            [quark.cogs.editor.analysis.scopes :refer [analyze-scopes]]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.stringz :refer [StringNode]]
            [rewrite-clj.node.keyword :refer [KeywordNode]]
            [rewrite-clj.node.token :refer [TokenNode]]
            [quark.cogs.editor.analyzer :refer [analyze-full]]
            [quark.cogs.editor.utils :refer [layouting-children essential-children node-walker node-interesting? leaf-nodes ancestor-count make-path make-zipper collect-all-right]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]]))

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
  (let [children (essential-children node)
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
