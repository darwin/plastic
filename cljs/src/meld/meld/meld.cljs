(ns meld.meld
  (:refer-clojure :exclude [descendants])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.node :as node]))

(defn set-top [meta top-id]
  (assoc meta :top top-id))

(defn get-top [meta]
  {:post [%]}
  (:top meta))

(defn get-top-node-id [meld]
  {:pre [meld]}
  (get-top (meta meld)))

(defn get-top-node [meld]
  {:pre [meld]}
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
        children (node/get-children node)]
    (if (seq children)
      (concat children (apply concat (map (partial descendants meld) children)))
      '())))

(defn dissoc-all! [meld& ids]
  (reduce dissoc! meld& ids))

(defn find-top-level-nodes-ids [meld]
  (let [sorter (fn [[_ a] [_ b]]
                 (let [start-a (node/get-start a)
                       start-b (node/get-start b)]
                   (compare start-a start-b)))
        has-parent? (fn [[_id node]]
                      (node/get-parent node))]
    (map first (sort sorter (remove has-parent? meld)))))

(defn define-unit [meld source name]
  (let [top-level-ids (find-top-level-nodes-ids meld)
        unit (node/make-unit top-level-ids source name)
        unit-id (:id unit)
        meld& (transient meld)
        meld&! (volatile! meld&)]
    (vswap! meld&! assoc! unit-id unit)
    (doseq [id top-level-ids]
      (vswap! meld&! assoc! id (assoc (get meld& id) :parent (:id unit))))
    (with-meta (persistent! @meld&!) (set-top {} unit-id))))

(defn get-compound-metrics [meld node]
  {:pre [node
         (node/compound? node)]}
  (let [children (node/get-children node)
        first-child (get-node meld (first children))
        last-child (get-node meld (last children))
        left-size (- (node/get-range-start first-child) (node/get-start node))
        right-size (- (node/get-range-end node) (node/get-end last-child))]
    [left-size right-size]))
