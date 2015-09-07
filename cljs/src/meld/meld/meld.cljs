(ns meld.meld
  (:refer-clojure :exclude [descendants])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.node :as node]))

(defn set-top [meta top-id]
  (assoc meta :top top-id))

(defn get-top [meta]
  {:post [%]}
  (:top meta))

(defn descendants [meld id]
  (let [node (get meld id)
        children (node/get-children node)]
    (if (seq children)
      (concat children (apply concat (map (partial descendants meld) children)))
      '())))

(defn dissoc-all! [meld* ids]
  (reduce dissoc! meld* ids))

(defn find-top-level-nodes-ids [meld]
  (map first (remove (fn [[id _node]] (:parent (get meld id))) meld)))

(defn define-unit [meld source name]
  (let [top-level-ids (find-top-level-nodes-ids meld)
        unit (node/make-unit top-level-ids source name)
        unit-id (:id unit)
        meld* (transient meld)
        meld*! (volatile! meld*)]
    (vswap! meld*! assoc! unit-id unit)
    (doseq [id top-level-ids]
      (vswap! meld*! assoc! id (assoc (get meld* id) :parent (:id unit))))
    (with-meta (persistent! @meld*!) (set-top {} unit-id))))

