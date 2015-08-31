(ns plastic.worker.editor.xforms.editing
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.editor.model.nodes :as nodes]
            [plastic.worker.editor.xforms.zipops :as ops]
            [plastic.worker.editor.model.xforming :refer [apply-op apply-ops]]
            [plastic.worker.editor.model :refer [valid-edit-point?]]
            [plastic.worker.editor.toolkit.id :as id]
            [rewrite-clj.node.keyword :refer [keyword-node]]
            [rewrite-clj.node.comment :refer [comment-node]]
            [rewrite-clj.node :as node]
            [plastic.worker.editor.parser.utils :as parser]
            [clojure.set :as set]))

(def get-node-id id/id-part)

(defn insert [editor edit-point & values]
  (if-not (id/spot? edit-point)
    (apply-op editor ops/insert-values-after-node values (get-node-id edit-point))
    (apply-op editor ops/insert-values-before-first-child-of-node values (get-node-id edit-point))))

(defn build-node [value initial-value]
  (let [{:keys [text mode]} value
        {:keys [type]} initial-value]
    (if (empty? value)
      nil
      (case type
        :comment (comment-node (str " " text))
        (condp = mode
          :symbol (node/coerce (symbol text))
          :keyword (keyword-node (keyword text))                                                                      ; TODO: investigate - coerce does not work for keywords?
          :string (node/coerce text)
          (throw "unknown editor mode passed to build-node:" mode))))))

(defn edit [editor edit-point puppets value initial-value]
  {:pre [(valid-edit-point? editor edit-point)]}
  (let [new-node (parser/assoc-node-id (build-node value initial-value))
        affected-node-ids (set/union #{(get-node-id edit-point)} puppets)]
    (apply-ops editor #(ops/commit-node-value %2 new-node %1) affected-node-ids)))

(defn enter [editor edit-point]
  {:pre [(valid-edit-point? editor edit-point)]}
  (let [linebreak-node (nodes/prepare-linebreak-node)]
    (insert editor edit-point linebreak-node)))

(defn alt-enter [editor edit-point]
  {:pre [(valid-edit-point? editor edit-point)]}
  (apply-op editor ops/remove-linebreak-before-node (get-node-id edit-point)))

(defn space [editor edit-point]
  {:pre [(valid-edit-point? editor edit-point)]}
  (let [placeholder-node (nodes/prepare-placeholder-node)]
    (insert editor edit-point placeholder-node)))

(defn backspace [editor edit-point]
  {:pre [(valid-edit-point? editor edit-point)]}
  (if-not (id/spot? edit-point)
    (apply-op editor ops/delete-node (get-node-id edit-point))
    (apply-op editor ops/splice-node (get-node-id edit-point))))

(defn delete [editor edit-point]
  {:pre [(valid-edit-point? editor edit-point)]}
  (if (id/spot? edit-point)
    (apply-op editor ops/remove-first-child-of-node (get-node-id edit-point))
    (apply-op editor ops/remove-right-siblink-of-node (get-node-id edit-point))))

(defn alt-delete [editor edit-point]
  {:pre [(valid-edit-point? editor edit-point)]}
  (apply-op editor ops/remove-left-siblink-of-node edit-point))

(defn open-compound [editor edit-point node-prepare-fn]
  {:pre [(valid-edit-point? editor edit-point)]}
  (let [placeholder-node (nodes/prepare-placeholder-node)
        compound-node (node-prepare-fn [placeholder-node])]
    (insert editor edit-point compound-node)))

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
  {:pre [(valid-edit-point? editor edit-point)]}
  (let [placeholder-node (nodes/prepare-placeholder-node)
        temporary-meta-data (nodes/prepare-keyword-node :meta)
        compound-node (nodes/prepare-meta-node [temporary-meta-data placeholder-node])]
    (insert editor edit-point compound-node)))

(defn open-quote [editor edit-point]
  (open-compound editor edit-point nodes/prepare-quote-node))

(defn open-deref [editor edit-point]
  (open-compound editor edit-point nodes/prepare-deref-node))

(defn insert-placeholder-as-first-child [editor edit-point]
  {:pre [(valid-edit-point? editor edit-point)]}
  (let [placeholder-node (nodes/prepare-placeholder-node)]
    (apply-op editor ops/insert-values-before-first-child-of-node (get-node-id edit-point) [placeholder-node])))
