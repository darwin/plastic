(ns meld.zip
  (:refer-clojure :exclude [find next remove replace])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.node :as node]
            [meld.meld :as meld]))

; -------------------------------------------------------------------------------------------------------------------

(defn update! [meld* k f & args]
  (let [o (get meld* k)]
    (assoc! meld* k (apply f o args))))

; -------------------------------------------------------------------------------------------------------------------

(defn zip [meld]
  [(transient meld) (:top meld)])

(defn unzip [loc]
  (persistent! (loc 0)))

; -------------------------------------------------------------------------------------------------------------------

(defn ^boolean end? [loc]
  (= :end (loc 1)))

(defn node [loc]
  (get (loc 0) (loc 1)))

(defn ^boolean branch? [loc]
  (seq (node/get-children (node loc))))

(defn children
  "Returns a seq of the children of node at loc, which must be a branch"
  [loc]
  (if (branch? loc)
    (node/get-children (node loc))
    (throw "called children on a leaf node")))

(defn parent [loc]
  (node/get-parent (node loc)))

(defn make-node
  "Returns a new branch node, given an existing node and new
  children. The loc is only used to supply the constructor."
  [node children]
  (node/set-children node children))

(defn top [loc]
  (let [meld* (loc 0)]
    [meld* (:top meld*)]))

(defn find [loc id]
  (let [meld* (loc 0)]
    (if (contains? meld* id)
      [meld* id])))

(defn insert-child [loc child-node]
  (let [[meld* id] loc
        child-id (node/get-id child-node)]
    [(-> meld*
       (update! id node/insert-child-leftmost child-id)
       (assoc! child-id (node/set-parent child-node id))) id]))

(defn insert-childs [loc nodes]
  (let [* (fn [loc node] (insert-child loc node))]
    (reduce * loc (reverse nodes))))

(defn insert-right [loc child-node]
  (let [[meld* id] loc
        parent-id (parent loc)
        child-id (node/get-id child-node)]
    [(-> meld*
       (update! parent-id node/insert-child-right id child-id)
       (assoc! child-id (node/set-parent child-node parent-id))) id]))

(defn insert-rights [loc nodes]
  (let [* (fn [loc node] (insert-right loc node))]
    (reduce * loc (reverse nodes))))

(defn down
  "Returns the loc of the leftmost child of the node at this loc, or
  nil if no children"
  [loc]
  (if-let [childs (children loc)]
    [(loc 0) (first childs)]))

(defn up
  "Returns the loc of the parent of the node at this loc, or nil if at
  the top"
  [loc]
  (if-let [parent-id (parent loc)]
    [(loc 0) parent-id]))

(defn right
  "Returns the loc of the right sibling of the node at this loc, or nil"
  [loc]
  (let [[meld* id] loc]
    (if-let [parent-id (parent loc)]
      (if-let [right-id (node/peek-right (get meld* parent-id) id)]
        [meld* right-id]))))

(defn left
  "Returns the loc of the left sibling of the node at this loc, or nil"
  [loc]
  (let [[meld* id] loc]
    (if-let [parent-id (parent loc)]
      (if-let [left-id (node/peek-left (get meld* parent-id) id)]
        [meld* left-id]))))

(defn rightmost
  "Returns the loc of the rightmost sibling of the node at this loc, or self"
  [loc]
  (let [[meld* id] loc
        parent-id (parent loc)
        parent (get meld* parent-id)
        result-id (node/rightmost-child parent)]
    (if result-id
      (if (identical? result-id id)
        loc
        [meld* result-id]))))

(defn leftmost
  "Returns the loc of the leftmost sibling of the node at this loc, or self"
  [loc]
  (let [[meld* id] loc
        parent-id (parent loc)
        parent (get meld* parent-id)
        result-id (node/rightmost-child parent)]
    (if result-id
      (if (identical? result-id id)
        loc
        [meld* result-id]))))

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
          [(loc 0) :end])))))

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
    (let [[meld* id] loc
          parent-id (parent loc)
          new-meld* (-> meld*
                      (update! parent-id node/remove-child id)
                      (meld/dissoc-all! (meld/descendants meld* id))
                      (dissoc! id))]
      [new-meld* (prev-loc 1)])
    (throw "Remove at top")))


(defn replace
  "Replaces the node at this loc, without moving"
  [loc node]
  (let [[meld* id] loc]
    (log "!" id node)
    (assert (identical? id (node/get-id node)))
    [(assoc! meld* id node) id]))

(defn edit
  "Replaces the node at this loc with the value of (f node args)"
  [loc f & args]
  (replace loc (apply f (node loc) args)))



;(defn ^boolean matching-end-loc? [end loc]
;  (= (:end (z/node loc)) end))
;
;(defn ^boolean whitespace-loc? [loc]
;  (#{:whitespace} (:type (z/node loc))))
;
;(defn ^boolean is-compound? [loc]
;  (if (z/end? loc)
;    false
;    (let [node (z/node loc)]
;      (#{:compound} (:type node)))))
;
;(defn get-node-end [loc]
;  (let [node (z/node loc)]
;    (:end node)))
;
;; -------------------------------------------------------------------------------------------------------------------
;
;(defn next-token [loc]
;  (let [next-loc (z/next loc)]
;    (first (drop-while is-compound? (iterate z/next next-loc)))))
;
;(defn take-all [loc]
;  (take-while (complement z/end?) (iterate z/next loc)))
