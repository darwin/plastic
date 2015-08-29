(ns plastic.worker.editor.model.nodes
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.editor.parser.utils :as parser]
            [rewrite-clj.node.whitespace :refer [whitespace-node]]
            [rewrite-clj.node.token :refer [token-node]]
            [rewrite-clj.node.keyword :refer [keyword-node]]
            [rewrite-clj.node.whitespace :refer [newline-node]]
            [rewrite-clj.node.meta :refer [meta-node]]
            [rewrite-clj.node.fn :refer [fn-node]]
            [rewrite-clj.node.seq :refer [list-node vector-node map-node set-node]]
            [rewrite-clj.node.reader-macro :refer [deref-node]]
            [rewrite-clj.node.quote :refer [quote-node]]))

(defn prepare-placeholder-node []
  (parser/assoc-node-id (token-node "" "")))

(defn prepare-linebreak-node []
  (parser/assoc-node-id (newline-node "\n")))

(defn prepare-keyword-node [k]
  (parser/assoc-node-id (keyword-node k)))

(defn prepare-list-node [children]
  (parser/assoc-node-id (list-node children)))

(defn prepare-vector-node [children]
  (parser/assoc-node-id (vector-node children)))

(defn prepare-map-node [children]
  (parser/assoc-node-id (map-node children)))

(defn prepare-set-node [children]
  (parser/assoc-node-id (set-node children)))

(defn prepare-fn-node [children]
  (parser/assoc-node-id (fn-node children)))

(defn prepare-meta-node [children]
  (parser/assoc-node-id (meta-node children)))

(defn prepare-quote-node [children]
  (parser/assoc-node-id (quote-node children)))

(defn prepare-deref-node [children]
  (parser/assoc-node-id (deref-node children)))
