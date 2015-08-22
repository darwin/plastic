(ns plastic.worker.editor.xforms.editing
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.editor.model.nodes :as nodes]
            [plastic.worker.editor.model.rewriting :as rewriting]
            [plastic.worker.editor.toolkit.id :as id]
            [rewrite-clj.node.keyword :refer [keyword-node]]
            [rewrite-clj.node :as node]
            [plastic.worker.editor.parser.utils :as parser]
            [clojure.set :as set]))

(def get-node-id id/id-part)

(defn insert-and-start-editing [editor edit-point & values]
  (if-not (id/spot? edit-point)
    (rewriting/insert-values-after editor (get-node-id edit-point) values)
    (rewriting/insert-values-before-first-child editor (get-node-id edit-point) values)))

(defn build-node [{:keys [text mode]}]
  (condp = mode
    :symbol (node/coerce (symbol text))
    :keyword (keyword-node (keyword text))                                                                            ; TODO: investigate - coerce does not work for keywords?
    :string (node/coerce text)
    (throw "unknown editor mode passed to build-node:" mode)))

(defn edit-node [editor edit-point puppets value]
  (let [new-node (parser/assoc-node-id (build-node value))
        affected-node-ids (set/union #{(get-node-id edit-point)} puppets)]
    (reduce #(rewriting/commit-node-value %1 %2 new-node) editor affected-node-ids)))

(defn enter [editor edit-point]
  (let [placeholder-node (nodes/prepare-placeholder-node)]
    (insert-and-start-editing editor edit-point (nodes/prepare-newline-node) placeholder-node)))

(defn alt-enter [editor edit-point]
  (rewriting/remove-linebreak-before editor (get-node-id edit-point)))

(defn space [editor edit-point]
  (let [placeholder-node (nodes/prepare-placeholder-node)]
    (insert-and-start-editing editor edit-point placeholder-node)))

(defn backspace [editor edit-point]
  (rewriting/delete-node editor (get-node-id edit-point)))

(defn delete [editor edit-point]
  (if (id/spot? edit-point)
    (rewriting/remove-first-child editor (get-node-id edit-point))
    (rewriting/remove-right-siblink editor (get-node-id edit-point))))

(defn alt-delete [editor edit-point]
  (rewriting/remove-left-siblink editor edit-point))

(defn open-compound [editor edit-point node-prepare-fn]
  (let [placeholder-node (nodes/prepare-placeholder-node)
        compound-node (node-prepare-fn [placeholder-node])]
    (insert-and-start-editing editor edit-point compound-node)))

(defn open-list [editor edit-point]
  (open-compound editor edit-point nodes/prepare-list-node))

(defn open-vector [editor edit-point]
  (open-compound editor edit-point nodes/prepare-vector-node))

(defn open-map [editor edit-point]
  (open-compound editor edit-point nodes/prepare-map-node))

(defn open-set [editor edit-point]
  (open-compound editor edit-point nodes/prepare-set-node))

(defn open-fn [editor edit-point]
  (open-compound editor edit-point nodes/prepare-fn-node))

(defn open-meta [editor edit-point]
  (let [placeholder-node (nodes/prepare-placeholder-node)
        temporary-meta-data (nodes/prepare-keyword-node :meta)
        compound-node (nodes/prepare-meta-node [temporary-meta-data placeholder-node])]
    (insert-and-start-editing editor edit-point compound-node)))

(defn open-quote [editor edit-point]
  (open-compound editor edit-point nodes/prepare-quote-node))

(defn open-deref [editor edit-point]
  (open-compound editor edit-point nodes/prepare-deref-node))

(defn insert-placeholder-as-first-child [editor edit-point]
  (let [placeholder-node (nodes/prepare-placeholder-node)]
    (rewriting/insert-values-before-first-child editor (get-node-id edit-point) [placeholder-node])))
