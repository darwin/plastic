(ns plastic.worker.editor.model.nodes
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.node :as node]
            [meld.core :as meld]))

(defn prepare-placeholder-node []
  (meld/make-tree (node/make-symbol "")))

(defn prepare-linebreak-node []
  (meld/make-tree (node/make-linebreak)))

(defn prepare-comment [content]
  (meld/make-tree (node/make-comment content)))

(defn prepare-symbol [v]
  (meld/make-tree (node/make-symbol (str v))))

(defn prepare-string [s]
  (meld/make-tree (node/make-string s)))

(defn prepare-keyword [k]
  (meld/make-tree (node/make-keyword k)))

(defn prepare-regexp [re]
  (meld/make-tree (node/make-regexp re)))

(defn prepare-list [children]
  (meld/make-tree (node/make-list) children))

(defn prepare-vector [children]
  (meld/make-tree (node/make-vector) children))

(defn prepare-map [children]
  (meld/make-tree (node/make-map) children))

(defn prepare-set [children]
  (meld/make-tree (node/make-set) children))

(defn prepare-fn [children]
  (error "implement prepare-fn-node")
  (meld/make-tree (node/make-list) children))

(defn prepare-meta [children]
  (error "implement prepare-meta-node")
  (meld/make-tree (node/make-list) children))

(defn prepare-quote [children]
  (error "implement prepare-quote-node")
  (meld/make-tree (node/make-list) children))

(defn prepare-deref [children]
  (error "implement prepare-deref-node")
  (meld/make-tree (node/make-list) children))
