(ns meld.zip
  (:refer-clojure :exclude [find next remove replace])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.node :as node]
            [meld.meld :as meld]
            [meld.util :refer [update!]]))

; -------------------------------------------------------------------------------------------------------------------

(defn zip [meld]
  (let [m (meta meld)]
    [(transient meld) (meld/get-top m) m]))

(defn unzip [loc]
  (let [meld (loc 0)]
    (with-meta (persistent! meld) (loc 2))))

; -------------------------------------------------------------------------------------------------------------------

(defn ^boolean end? [loc]
  (= :end (loc 1)))

(defn node [loc]
  {:post [%]}
  (get (loc 0) (loc 1)))

(defn ^boolean branch? [loc]
  (node/get-children (node loc)))

(defn children
  "Returns a seq of the children of node at loc, which must be a branch"
  [loc]
  (if (branch? loc)
    (node/get-children (node loc))
    (throw "called children on a leaf node")))

(defn parent [loc]
  (node/get-parent (node loc)))

(defn top [loc]
  (assoc loc 1 (meld/get-top (loc 2))))

(defn find [loc id]
  (if (contains? (loc 0) id)
    (assoc loc 1 id)))

(defn insert-child [loc child-node]
  (let [[meld& id] loc
        child-id (node/get-id child-node)
        new-meld& (-> meld&
                    (update! id node/insert-child-leftmost child-id)
                    (assoc! child-id (node/set-parent child-node id)))]
    (assoc loc 0 new-meld&)))

(defn insert-childs [loc nodes]
  (let [* (fn [loc node] (insert-child loc node))]
    (reduce * loc (reverse nodes))))

(defn insert-right [loc child-node]
  (let [[meld& id] loc
        parent-id (parent loc)
        child-id (node/get-id child-node)
        new-meld& (-> meld&
                    (update! parent-id node/insert-child-right id child-id)
                    (assoc! child-id (node/set-parent child-node parent-id)))]
    (assoc loc 0 new-meld&)))

(defn insert-rights [loc nodes]
  (let [* (fn [loc node] (insert-right loc node))]
    (reduce * loc (reverse nodes))))

(defn down
  "Returns the loc of the leftmost child of the node at this loc, or
  nil if no children"
  [loc]
  (if-let [childs (children loc)]
    (assoc loc 1 (first childs))))

(defn up
  "Returns the loc of the parent of the node at this loc, or nil if at
  the top"
  [loc]
  (if-let [parent-id (parent loc)]
    (assoc loc 1 parent-id)))

(defn right
  "Returns the loc of the right sibling of the node at this loc, or nil"
  [loc]
  (let [[meld& id meta] loc]
    (if-let [parent-id (parent loc)]
      (if-let [right-id (node/peek-right (get meld& parent-id) id)]
        [meld& right-id meta]))))

(defn left
  "Returns the loc of the left sibling of the node at this loc, or nil"
  [loc]
  (let [[meld& id meta] loc]
    (if-let [parent-id (parent loc)]
      (if-let [left-id (node/peek-left (get meld& parent-id) id)]
        [meld& left-id meta]))))

(defn rightmost
  "Returns the loc of the rightmost sibling of the node at this loc, or self"
  [loc]
  (let [[meld& id meta] loc
        parent-id (parent loc)
        parent (get meld& parent-id)
        result-id (node/rightmost-child parent)]
    (if result-id
      (if (identical? result-id id)
        loc
        [meld& result-id meta]))))

(defn leftmost
  "Returns the loc of the leftmost sibling of the node at this loc, or self"
  [loc]
  (let [[meld& id meta] loc
        parent-id (parent loc)
        parent (get meld& parent-id)
        result-id (node/rightmost-child parent)]
    (if result-id
      (if (identical? result-id id)
        loc
        [meld& result-id meta]))))

(defn next
  "Moves to the next loc in the hierarchy, depth-first. When reaching
  the end, returns a distinguished loc detectable via end?. If already
  at the end, stays there."
  [loc]
  (if (end? loc)
    loc
    (or
      (and (branch? loc) (down loc))
      (right loc)
      (loop [p loc]
        (if-let [up-loc (up p)]
          (or (right up-loc) (recur up-loc))
          [(loc 0) :end (loc 2)])))))

(defn prev
  "Moves to the previous loc in the hierarchy, depth-first. If already
  at the root, returns nil."
  [loc]
  (if-let [lloc (left loc)]
    (loop [loc lloc]
      (if-let [child (and (branch? loc) (down loc))]
        (recur (rightmost child))
        loc))
    (up loc)))

(defn remove
  "Removes the node at loc, returning the loc that would have preceded
  it in a depth-first walk."
  [loc]
  (if-let [prev-loc (prev loc)]
    (let [[meld& id meta] loc
          parent-id (parent loc)
          new-meld& (-> meld&
                      (update! parent-id node/remove-child id)
                      (meld/dissoc-all! (meld/descendants meld& id))
                      (dissoc! id))]
      [new-meld& (prev-loc 1) meta])
    (throw "Remove at top")))

(defn replace
  "Replaces the node at this loc, without moving"
  [loc node]
  (let [[meld& id] loc
        new-meld& (-> meld&
                    (assoc! id node))]
    (assert (identical? id (node/get-id node)))
    (assoc loc 0 new-meld&)))

(defn edit
  "Replaces the node at this loc with the value of (f node args)"
  [loc f & args]
  (replace loc (apply f (node loc) args)))
