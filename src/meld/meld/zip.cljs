(ns meld.zip
  (:refer-clojure :exclude [meta find next remove replace descendants ancestors
                            string? symbol? list? map? vector? set?])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.node :as node]
            [meld.core :as meld]
            [meld.util :refer [update!]]))

(defn meld [loc]
  (get loc 0))

(defn id [loc]
  (get loc 1 :end))

(defn meta [loc]
  (get loc 2))

(defn top-id [loc]
  (get loc 3))

(defn set-id [loc id]
  (assoc loc 1 id))

(defn ^boolean end? [loc]
  (keyword-identical? :end (id loc)))

(defn ^boolean good? [loc]
  (and loc (number? (id loc))))

(defn node [loc]
  {:post [%]}
  (get (meld loc) (id loc)))

(defn make-loc [meld& id meta top-id]
  [meld& id meta top-id])

; -------------------------------------------------------------------------------------------------------------------
; mirror node API on locs

(defn get-source [loc]
  (node/get-source (node loc)))

(defn get-content [loc]
  (node/get-content (node loc)))

(defn get-type [loc]
  (node/get-type (node loc)))

(defn get-tag [loc]
  (node/get-tag (node loc)))

(defn get-sexpr [loc]
  (node/get-sexpr (node loc)))

(defn ^boolean compound? [loc]
  (node/compound? (node loc)))

(defn ^boolean whitespace? [loc]
  (node/whitespace? (node loc)))

(defn ^boolean comment? [loc]
  (node/comment? (node loc)))

(defn ^boolean linebreak? [loc]
  (node/linebreak? (node loc)))

(defn ^boolean string? [loc]
  (node/string? (node loc)))

(defn ^boolean symbol? [loc]
  (node/symbol? (node loc)))

(defn ^boolean list? [loc]
  (node/list? (node loc)))

(defn ^boolean vector? [loc]
  (node/vector? (node loc)))

(defn ^boolean map? [loc]
  (node/map? (node loc)))

(defn ^boolean set? [loc]
  (node/set? (node loc)))

(defn ^boolean unit? [loc]
  (node/unit? (node loc)))

; -------------------------------------------------------------------------------------------------------------------

(defn zip
  "Returns a new zipper from meld optionally limited to a subtree"
  ([meld] (zip meld nil))
  ([meld subtree-id]
   (let [meta (cljs.core/meta meld)
         top-id (or subtree-id (meld/get-top-node-id meld))]
     (make-loc (transient meld) top-id meta top-id))))

(defn unzip [loc]
  (with-meta (persistent! (meld loc)) (meta loc)))

(defn subzip [loc]
  "Return a new zipper limited to a subtree at current loc"
  {:pre [(good? loc)]}
  (assoc loc 3 (id loc)))

; -------------------------------------------------------------------------------------------------------------------

(defn ^boolean branch? [loc]
  (node/compound? (node loc)))

(defn children
  "Returns a seq of the children of node at loc, which must be a branch"
  [loc]
  {:pre [(branch? loc)]}
  (node/get-children (node loc)))

(defn parent-ignoring-subzip-boundary [loc]
  (node/get-parent (node loc)))

(defn parent [loc]
  (if-not (identical? (id loc) (top-id loc))
    (node/get-parent (node loc))))

(defn top [loc]
  (assoc loc 1 (top-id loc)))

(defn find [loc id]
  (if (contains? (loc 0) id)
    (assoc loc 1 id)))

(defn down
  "Returns the loc of the leftmost child of the node at this loc, or nil if no children"
  [loc]
  (let [node (node loc)]
    (if (node/compound? node)
      (if-let [first-child (first (node/get-children node))]
        (assoc loc 1 first-child)))))

(defn up
  "Returns the loc of the parent of the node at this loc, or nil if at the top"
  [loc]
  (if-let [parent-id (parent loc)]
    (assoc loc 1 parent-id)))

(defn right
  "Returns the loc of the right sibling of the node at this loc, or nil"
  [loc]
  (let [[meld& id] loc]
    (if-let [parent-id (parent loc)]
      (if-let [right-id (node/peek-right (get meld& parent-id) id)]
        (assoc loc 1 right-id)))))

(defn left
  "Returns the loc of the left sibling of the node at this loc, or nil"
  [loc]
  (let [[meld& id] loc]
    (if-let [parent-id (parent loc)]
      (if-let [left-id (node/peek-left (get meld& parent-id) id)]
        (assoc loc 1 left-id)))))

(defn rightmost
  "Returns the loc of the rightmost sibling of the node at this loc, or self"
  [loc]
  (let [[meld& id] loc
        parent-id (parent loc)]
    (if parent-id
      (let [parent (get meld& parent-id)
            result-id (node/rightmost-child parent)]
        (if result-id
          (if (identical? result-id id)
            loc
            (assoc loc 1 result-id)))))))

(defn leftmost
  "Returns the loc of the leftmost sibling of the node at this loc, or self"
  [loc]
  (let [[meld& id] loc
        parent-id (parent loc)]
    (if parent-id
      (let [parent (get meld& parent-id)
            result-id (node/leftmost-child parent)]
        (if result-id
          (if (identical? result-id id)
            loc
            (assoc loc 1 result-id)))))))

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
          (assoc loc 1 :end))))))

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

; -------------------------------------------------------------------------------------------------------------------

(defn descendant-locs [loc]
  (let [stop-id (id (right loc))]
    (take-while #(not= (id %) stop-id) (iterate next (next loc)))))

(defn descendants [loc]
  (map id (descendant-locs loc)))

(defn child-locs [loc]
  (->> loc
    (down)
    (iterate right)
    (take-while good?)))

(defn ancestor-locs [loc]
  (take-while good? (iterate up loc)))

(defn ancestors [loc]
  (map id (ancestor-locs loc)))

; -------------------------------------------------------------------------------------------------------------------

(defn mark-new-revision! [meld& id]
  (let [nodes-above-me (meld/all-ancestor-nodes meld& id)
        revisioning-node-ids-above-me (map node/get-id (filter node/revisioning? nodes-above-me))
        * (fn [meld& id] (update! meld& id node/inc-revision))]
    (reduce * meld& revisioning-node-ids-above-me)))

; -------------------------------------------------------------------------------------------------------------------

(defn insert-child [loc child-tree]
  (let [[meld& id] loc
        child-node (meld/get-tree-node child-tree)
        child-id (node/get-id child-node)
        new-meld& (-> meld&
                    (meld/flatten-tree-into-meld child-tree)
                    (update! id node/insert-child-leftmost child-id)
                    (update! child-id node/set-parent id)
                    (mark-new-revision! id))]
    (assoc loc 0 new-meld&)))

(defn insert-childs [loc trees]
  (let [* (fn [loc tree] (insert-child loc tree))]
    (reduce * loc (reverse trees))))

(defn insert-right [loc child-tree]
  (let [[meld& id] loc
        parent-id (parent loc)
        _ (assert parent-id)
        child-node (meld/get-tree-node child-tree)
        child-id (node/get-id child-node)
        new-meld& (-> meld&
                    (meld/flatten-tree-into-meld child-tree)
                    (update! parent-id node/insert-child-right id child-id)
                    (update! child-id node/set-parent parent-id)
                    (mark-new-revision! parent-id))]
    (assoc loc 0 new-meld&)))

(defn insert-rights [loc trees]
  (let [* (fn [loc tree] (insert-right loc tree))]
    (reduce * loc (reverse trees))))

(defn insert-left [loc child-tree]
  (let [[meld& id] loc
        parent-id (parent loc)
        _ (assert parent-id)
        child-node (meld/get-tree-node child-tree)
        child-id (node/get-id child-node)
        new-meld& (-> meld&
                    (meld/flatten-tree-into-meld child-tree)
                    (update! parent-id node/insert-child-left id child-id)
                    (update! child-id node/set-parent parent-id)
                    (mark-new-revision! parent-id))]
    (assoc loc 0 new-meld&)))

(defn insert-lefts [loc trees]
  (let [* (fn [loc tree] (insert-left loc tree))]
    (reduce * loc trees)))

(defn remove
  "Removes the node at loc, returning the loc that would have preceded it in a depth-first walk."
  [loc]
  (let [prev-loc (prev loc)]
    (assert prev-loc "Remove at top")
    (let [[meld& id] loc
          parent-id (parent loc)
          _ (assert parent-id)
          new-meld& (-> meld&
                      (update! parent-id node/remove-child id)
                      (meld/dissoc-all! (meld/descendants meld& id))
                      (dissoc! id)
                      (mark-new-revision! parent-id))]
      (assoc prev-loc 0 new-meld&))))

(defn replace
  "Replaces the node at this loc with a new tree, without moving"
  [loc tree]
  (let [[meld& id] loc
        new-node (meld/get-tree-node tree)
        new-node-id (node/get-id new-node)
        parent-id (parent-ignoring-subzip-boundary loc)                                                               ; note: can be nil and that's a valid case for root node
        new-meld& (-> meld&
                    (meld/flatten-tree-into-meld tree)
                    (update! new-node-id node/set-parent parent-id)
                    (update! parent-id node/replace-child id new-node-id)
                    (mark-new-revision! new-node-id))]
    (-> loc
      (assoc 0 new-meld&)
      (assoc 1 new-node-id))))

(defn edit*
  "Edits the node at this loc by replacing it with a new node in-place, not changing any parent/child relationships"
  [loc new-node]
  (let [[meld& id] loc
        old-node (node loc)
        ; new-node must have same id, parent and children
        _ (assert (identical? (node/get-id old-node) (node/get-id new-node)))
        _ (assert (identical? (node/get-parent old-node) (node/get-parent new-node)))
        _ (assert (= (node/get-children old-node) (node/get-children new-node)))
        new-meld& (-> meld&
                    (assoc! id new-node)
                    (mark-new-revision! id))]
    (assoc loc 0 new-meld&)))

(defn edit
  "Edits the node at this loc with the value of (f node args)"
  [loc f & args]
  (edit* loc (apply f (node loc) args)))

(defn walk [loc accum f & args]
  (let [walk-child (fn [accum child-id]
                     (apply walk (assoc loc 1 child-id) accum f args))
        node (node loc)]
    (if-let [children (if (node/compound? node) (node/get-children node))]
      (let [accum-after-enter (apply f accum node :enter args)
            accum-after-children (reduce walk-child accum-after-enter children)
            accum-after-leave (apply f accum-after-children node :leave args)]
        accum-after-leave)
      (apply f accum node :token args))))

; -------------------------------------------------------------------------------------------------------------------

(defn ^boolean top? [loc]
  (identical? (id loc) (top-id loc)))

(defn ^boolean child-of-top? [loc]
  (top? (parent loc)))

(defn ^boolean leftmost? [loc]
  (identical? (id (leftmost loc)) (id loc)))

(defn ^boolean rightmost? [loc]
  (identical? (id (rightmost loc)) (id loc)))

(defn lefts
  "Returns a seq of the left siblings of this loc"
  [loc]
  (let [[meld& id] loc
        parent-id (parent loc)]
    (assert parent-id "called lefts at top")
    (map (partial meld/get-node meld&) (node/lefts (get meld& parent-id) id))))

(defn rights
  "Returns a seq of the right siblings of this loc"
  [loc]
  (let [[meld& id] loc
        parent-id (parent loc)]
    (assert parent-id "called rights at top")
    (map (partial meld/get-node meld&) (node/rights (get meld& parent-id) id))))

; -------------------------------------------------------------------------------------------------------------------

(defn desc [loc]
  (if-not (good? loc)
    "nil"
    (let [node (node loc)]
      (node/get-desc node))))

; -------------------------------------------------------------------------------------------------------------------

(defn loc->path [loc]
  (if-let [parent-loc (up loc)]
    (conj (loc->path parent-loc) (count (lefts loc)))
    []))

(defn inc-path [path]
  (update path (dec (count path)) inc))

(defn path->loc [path loc]
  (if (and path (good? loc))
    (if (empty? path)
      loc
      (let [down-loc (down loc)
            child-loc (nth (iterate right down-loc) (first path))]
        (if (good? child-loc)
          (recur (rest path) child-loc))))))

(defn path-compare [path1 path2]
  (loop [p1 path1
         p2 path2]
    (cond
      (and (empty? p1) (not (empty? p2))) -1
      (and (not (empty? p1)) (empty? p2)) 1
      :else (let [c (compare (first p1) (first p2))]
              (if-not (zero? c)
                c
                (recur (rest p1) (rest p2)))))))

(defn path< [path1 path2]
  (neg? (path-compare path1 path2)))

(defn path<= [path1 path2]
  (let [res (path-compare path1 path2)]
    (or (zero? res) (neg? res))))

; -------------------------------------------------------------------------------------------------------------------

(defn take-all [f loc]
  (take-while good? (iterate f loc)))