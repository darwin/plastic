(ns meld.core
  (:refer-clojure :exclude [descendants ancestors])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.node :as node]
            [meld.util :refer [update!]]))

(defn set-top [meta top-id]
  (assoc meta :top top-id))

(defn get-top [meta]
  {:post [%]}
  (:top meta))

(defn get-top-node-id [meld]
  {:pre [meld]}
  (get-top (meta meld)))

(defn get-top-node [meld]
  {:pre  [meld]
   :post [%]}
  (get meld (get-top-node-id meld)))

(defn get-source [meld]
  {:pre [meld]}
  (node/get-source (get-top-node meld)))

(defn nodes-count [meld]
  {:pre [meld]}
  (count (keys meld)))

(defn get-node [meld id]
  {:pre [meld]}
  (get meld id))

(defn descendants [meld id]
  (let [node (get meld id)
        children (if (node/compound? node) (node/get-children node))]
    (if (seq children)
      (concat children (apply concat (map (partial descendants meld) children)))
      (list))))

(defn dissoc-all! [meld& ids]
  (reduce dissoc! meld& ids))

(defn all-ancestor-nodes [meld start-id]
  (loop [id start-id
         res []]
    (let [node (get-node meld id)]
      (assert node)
      (if-let [parent-id (node/get-parent node)]                                                                      ; ignoring-subtree-boundary
        (recur parent-id (conj res node))
        res))))

(defn get-compound-metrics [meld node]
  {:pre [node
         (node/compound? node)]}
  (let [children (node/get-children node)
        first-child (get-node meld (first children))
        last-child (get-node meld (last children))
        left-size (- (node/get-range-start first-child) (node/get-start node))
        right-size (- (node/get-range-end node) (node/get-end last-child))]
    [left-size right-size]))

; -------------------------------------------------------------------------------------------------------------------

(defn ancestors* [meld id]
  (let [node (get-node meld id)]
    (if-let [parent-id (node/get-parent node)]
      (conj (ancestors* meld parent-id) id)
      [id])))

(defn ancestors [meld id]
  (butlast (ancestors* meld id)))

; -------------------------------------------------------------------------------------------------------------------

; tree is a convenience structure for building node tree to be later merged into meld
; a tree node is `[node vec-of-children-tree-nodes]` or just `node` if leaf
; merge-tree does the work of flattening this nested structure and assigning parent/children links between nodes
; it does not assign parent to the root node of the tree, you have to do it yourself after merging

(defn make-tree
  ([node] (make-tree node nil))
  ([node children]
   {:pre [node]}
   (if children
     [node children]
     node)))

(defn destructure-tree [tree]
  (if (vector? tree)
    tree
    [tree nil]))

(defn get-tree-node [tree]
  (first (destructure-tree tree)))

(defn get-tree-children [tree]
  (second (destructure-tree tree)))

(defn flatten-tree-into-meld [meld& tree]
  (let [[node children] (destructure-tree tree)
        node-id (node/get-id node)
        * (fn [[meld& ids] child-tree]
            (let [child-node (get-tree-node child-tree)
                  child-id (node/get-id child-node)
                  flatten-meld& (flatten-tree-into-meld meld& child-tree)
                  new-meld& (update! flatten-meld& child-id node/set-parent node-id)]
              [new-meld& (conj ids child-id)]))
        [flattened-meld& child-ids] (reduce * [meld& []] children)
        node-with-children (if (seq child-ids) (node/set-children node child-ids) node)]
    (assoc! flattened-meld& node-id node-with-children)))

(defn flatten-tree [tree]
  (-> {}
    (transient)
    (flatten-tree-into-meld tree)
    (persistent!)))

; this is a low-level operation, you have to properly assign parent of top tree node
(defn merge-tree [meld tree]
  (let [flattened-tree (flatten-tree tree)]
    (merge meld flattened-tree)))