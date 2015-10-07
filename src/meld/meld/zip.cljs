(ns meld.zip
  (:refer-clojure :exclude [find next remove replace descendants ancestors
                            string? symbol? list? map? vector? set?])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.node :as node]
            [meld.core :as meld]
            [meld.util :refer [update! dissoc-all!]]))

(defn make-loc [meld& id aux top-id root-id next-id]
  [meld& id aux top-id root-id next-id])

(defn get-meld& [loc]
  (get loc 0))

(defn set-meld& [loc meld&]
  (assoc loc 0 meld&))

(defn get-id [loc]
  (get loc 1 :end))

(defn set-id [loc id]
  (assoc loc 1 id))

(defn get-aux [loc]
  (get loc 2))

(defn set-aux [loc aux]
  (assoc loc 2 aux))

(defn get-top-id [loc]
  (get loc 3))

(defn set-top-id [loc top-id]
  (assoc loc 3 top-id))

(defn get-root-id [loc]
  (get loc 4))

(defn set-root-id [loc root-id]
  (assoc loc 4 root-id))

(defn get-next-id [loc]
  (get loc 5))

(defn set-next-id [loc next-id]
  (assoc loc 5 next-id))

(defn ^boolean end? [loc]
  (keyword-identical? :end (get-id loc)))

(defn ^boolean good? [loc]
  (and loc (number? (get-id loc))))

(defn get-node [loc]
  {:post [%]}
  (get (get-meld& loc) (get-id loc)))

; -------------------------------------------------------------------------------------------------------------------
; mirror node API on locs

(defn get-source [loc]
  (node/get-source (get-node loc)))

(defn get-content [loc]
  (node/get-content (get-node loc)))

(defn get-type [loc]
  (node/get-type (get-node loc)))

(defn get-tag [loc]
  (node/get-tag (get-node loc)))

(defn get-sexpr [loc]
  (node/get-sexpr (get-node loc)))

(defn ^boolean compound? [loc]
  (node/compound? (get-node loc)))

(defn ^boolean whitespace? [loc]
  (node/whitespace? (get-node loc)))

(defn ^boolean comment? [loc]
  (node/comment? (get-node loc)))

(defn ^boolean linebreak? [loc]
  (node/linebreak? (get-node loc)))

(defn ^boolean string? [loc]
  (node/string? (get-node loc)))

(defn ^boolean symbol? [loc]
  (node/symbol? (get-node loc)))

(defn ^boolean list? [loc]
  (node/list? (get-node loc)))

(defn ^boolean vector? [loc]
  (node/vector? (get-node loc)))

(defn ^boolean map? [loc]
  (node/map? (get-node loc)))

(defn ^boolean set? [loc]
  (node/set? (get-node loc)))

(defn ^boolean unit? [loc]
  (node/unit? (get-node loc)))

; -------------------------------------------------------------------------------------------------------------------

(defn zip
  "Returns a new zipper from meld optionally limited to a subtree"
  ([meld] (zip meld nil))
  ([meld top-id]
   (meld/sanity-check meld)
   (let [aux (meta meld)
         root-id (meld/get-root-node-id meld)
         next-id (meld/get-next-node-id meld)
         top-id (or top-id root-id)]
     (make-loc (transient meld) top-id aux top-id root-id next-id))))

(defn unzip [loc]
  "Turns zipper back into meld structure commiting all potential changes"
  (-> (with-meta (persistent! (get-meld& loc)) (get-aux loc))
    (meld/set-next-node-id (get-next-id loc))
    (meld/set-root-node-id (get-root-id loc))
    (meld/sanity-check)))

(defn subzip [loc]
  "Return a new zipper limited to a subtree at current loc"
  {:pre [(good? loc)]}
  (set-top-id loc (get-id loc)))

; -------------------------------------------------------------------------------------------------------------------

(defn ^boolean branch? [loc]
  (node/compound? (get-node loc)))

(defn children
  "Returns a seq of the children of node at loc, which must be a branch"
  [loc]
  {:pre [(branch? loc)]}
  (node/get-children (get-node loc)))

(defn get-parent-id-ignoring-subzip-boundary [loc]
  (node/get-parent (get-node loc)))

(defn parent [loc]
  (if-not (identical? (get-id loc) (get-top-id loc))
    (node/get-parent (get-node loc))))

(defn top [loc]
  (set-id loc (get-top-id loc)))

(defn root [loc]
  (set-id loc (get-root-id loc)))

(defn find [loc id]
  {:pre [id]}
  (if (contains? (get-meld& loc) id)
    (set-id loc id)))

(defn down
  "Returns the loc of the leftmost child of the node at this loc, or nil if no children"
  [loc]
  (let [node (get-node loc)]
    (if (node/compound? node)
      (if-let [first-child-id (first (node/get-children node))]
        (set-id loc first-child-id)))))

(defn up
  "Returns the loc of the parent of the node at this loc, or nil if at the top"
  [loc]
  (if-let [parent-id (parent loc)]
    (set-id loc parent-id)))

(defn right
  "Returns the loc of the right sibling of the node at this loc, or nil"
  [loc]
  (let [meld& (get-meld& loc)
        id (get-id loc)]
    (if-let [parent-id (parent loc)]
      (if-let [right-id (node/peek-right (get meld& parent-id) id)]
        (set-id loc right-id)))))

(defn left
  "Returns the loc of the left sibling of the node at this loc, or nil"
  [loc]
  (let [meld& (get-meld& loc)
        id (get-id loc)]
    (if-let [parent-id (parent loc)]
      (if-let [left-id (node/peek-left (get meld& parent-id) id)]
        (set-id loc left-id)))))

(defn rightmost
  "Returns the loc of the rightmost sibling of the node at this loc, or self"
  [loc]
  (let [meld& (get-meld& loc)
        id (get-id loc)
        parent-id (parent loc)]
    (if parent-id
      (let [parent (get meld& parent-id)
            result-id (node/rightmost-child parent)]
        (if result-id
          (if (identical? result-id id)
            loc
            (set-id loc result-id)))))))

(defn leftmost
  "Returns the loc of the leftmost sibling of the node at this loc, or self"
  [loc]
  (let [meld& (get-meld& loc)
        id (get-id loc)
        parent-id (parent loc)]
    (if parent-id
      (let [parent (get meld& parent-id)
            result-id (node/leftmost-child parent)]
        (if result-id
          (if (identical? result-id id)
            loc
            (set-id loc result-id)))))))

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
          (set-id loc :end))))))

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

(defn take-all [f loc]
  (->> loc
    (iterate f)
    (take-while good?)))

(defn take-all-next [loc]
  (take-all next loc))

(defn take-subtree [loc]
  (let [stop-id (get-id (right loc))]
    (->> loc
      (iterate next)
      (take-while #(not= (get-id %) stop-id)))))

(defn take-descendants [loc]
  (rest (take-subtree loc)))

(defn take-children [loc]
  (->> loc
    (down)
    (iterate right)
    (take-while good?)))

(defn take-ancestors [loc]
  (->> loc
    (iterate up)
    (take-while good?)))

; -------------------------------------------------------------------------------------------------------------------
; deprecated, use take-* functions

(defn descendant-locs [loc]
  (let [stop-id (get-id (right loc))]
    (take-while #(not= (get-id %) stop-id) (iterate next (next loc)))))

(defn descendants [loc]
  (map get-id (descendant-locs loc)))

(defn child-locs [loc]
  (->> loc
    (down)
    (iterate right)
    (take-while good?)))

(defn ancestor-locs [loc]
  (take-while good? (iterate up loc)))

(defn ancestors [loc]
  (map get-id (ancestor-locs loc)))

; -------------------------------------------------------------------------------------------------------------------

(defn mark-new-revision! [meld& id]
  (let [nodes-above-me (meld/all-ancestor-nodes meld& id)
        revisioning-node-ids-above-me (map node/get-id (filter node/revisioning? nodes-above-me))
        * (fn [meld& id] (update! meld& id node/inc-revision))]
    (reduce * meld& revisioning-node-ids-above-me)))

; -------------------------------------------------------------------------------------------------------------------

(defn insert-child [loc child-tree]
  (let [meld& (get-meld& loc)
        id (get-id loc)
        child-id (get-next-id loc)
        [flattened-meld& next-id] (meld/flatten-tree-into-meld& meld& child-tree child-id)
        new-meld& (-> flattened-meld&
                    (update! id node/insert-child-leftmost child-id)
                    (update! child-id node/set-parent id)
                    (mark-new-revision! id))]
    (-> loc
      (set-meld& new-meld&)
      (set-next-id next-id))))

(defn insert-childs [loc trees]
  (let [* (fn [loc tree] (insert-child loc tree))]
    (reduce * loc (reverse trees))))

(defn insert-right [loc child-tree]
  (let [meld& (get-meld& loc)
        id (get-id loc)
        parent-id (parent loc)
        _ (assert parent-id)
        child-id (get-next-id loc)
        [flattened-meld& next-id] (meld/flatten-tree-into-meld& meld& child-tree child-id)
        new-meld& (-> flattened-meld&
                    (update! parent-id node/insert-child-right id child-id)
                    (update! child-id node/set-parent parent-id)
                    (mark-new-revision! parent-id))]
    (-> loc
      (set-meld& new-meld&)
      (set-next-id next-id))))

(defn insert-rights [loc trees]
  (let [* (fn [loc tree] (insert-right loc tree))]
    (reduce * loc (reverse trees))))

(defn insert-left [loc child-tree]
  (let [meld& (get-meld& loc)
        id (get-id loc)
        parent-id (parent loc)
        _ (assert parent-id)
        child-id (get-next-id loc)
        [flattened-meld& next-id] (meld/flatten-tree-into-meld& meld& child-tree child-id)
        new-meld& (-> flattened-meld&
                    (update! parent-id node/insert-child-left id child-id)
                    (update! child-id node/set-parent parent-id)
                    (mark-new-revision! parent-id))]
    (-> loc
      (set-meld& new-meld&)
      (set-next-id next-id))))

(defn insert-lefts [loc trees]
  (let [* (fn [loc tree] (insert-left loc tree))]
    (reduce * loc trees)))

(defn remove
  "Removes the node at loc, returning the loc that would have preceded it in a depth-first walk."
  [loc]
  (let [prev-loc (prev loc)]
    (assert prev-loc "Remove at top")
    (let [meld& (get-meld& loc)
          id (get-id loc)
          parent-id (parent loc)
          _ (assert parent-id)
          new-meld& (-> meld&
                      (update! parent-id node/remove-child id)
                      (dissoc-all! (meld/descendants meld& id))
                      (dissoc! id)
                      (mark-new-revision! parent-id))]
      (set-meld& prev-loc new-meld&))))

(defn replace
  "Replaces the node at this loc with a new tree, without moving"
  [loc tree]
  (let [meld& (get-meld& loc)
        id (get-id loc)
        parent-id (get-parent-id-ignoring-subzip-boundary loc)                                                        ; note: can be nil and that's a valid case for root node
        new-node-id (get-next-id loc)
        [flattened-meld& next-id] (meld/flatten-tree-into-meld& meld& tree new-node-id)
        new-meld& (-> flattened-meld&
                    (update! new-node-id node/set-parent parent-id)
                    (update! parent-id node/replace-child id new-node-id)
                    (mark-new-revision! new-node-id))]
    (-> loc
      (set-meld& new-meld&)
      (set-id new-node-id)
      (set-next-id next-id))))

(defn edit*
  "Edits the node at this loc by replacing it with a new node in-place, not changing any parent/child relationships"
  [loc new-node]
  (let [meld& (get-meld& loc)
        id (get-id loc)
        old-node (get-node loc)
        ; new-node must have same id, parent and children
        _ (assert (identical? id (node/get-id old-node)))
        _ (assert (identical? (node/get-id old-node) (node/get-id new-node)))
        _ (assert (identical? (node/get-parent old-node) (node/get-parent new-node)))
        _ (assert (= (node/get-children old-node) (node/get-children new-node)))
        new-meld& (-> meld&
                    (assoc! id new-node)
                    (mark-new-revision! id))]
    (set-meld& loc new-meld&)))

(defn edit
  "Edits the node at this loc with the value of (f node args)"
  [loc f & args]
  (edit* loc (apply f (get-node loc) args)))

(defn walk [loc accum f & args]
  (let [walk-child (fn [accum child-id]
                     (apply walk (set-id loc child-id) accum f args))
        node (get-node loc)]
    (if-let [children (if (node/compound? node) (node/get-children node))]
      (let [accum-after-enter (apply f accum node :enter args)
            accum-after-children (reduce walk-child accum-after-enter children)
            accum-after-leave (apply f accum-after-children node :leave args)]
        accum-after-leave)
      (apply f accum node :token args))))

; -------------------------------------------------------------------------------------------------------------------

(defn ^boolean top? [loc]
  (identical? (get-id loc) (get-top-id loc)))

(defn ^boolean child-of-top? [loc]
  (top? (parent loc)))

(defn ^boolean leftmost? [loc]
  (identical? (get-id (leftmost loc)) (get-id loc)))

(defn ^boolean rightmost? [loc]
  (identical? (get-id (rightmost loc)) (get-id loc)))

(defn lefts
  "Returns a seq of the left siblings of this loc"
  [loc]
  (let [meld& (get-meld& loc)
        id (get-id loc)
        parent-id (parent loc)]
    (assert parent-id "called lefts at top")
    (map (partial meld/get-node meld&) (node/lefts (get meld& parent-id) id))))

(defn rights
  "Returns a seq of the right siblings of this loc"
  [loc]
  (let [meld& (get-meld& loc)
        id (get-id loc)
        parent-id (parent loc)]
    (assert parent-id "called rights at top")
    (map (partial meld/get-node meld&) (node/rights (get meld& parent-id) id))))

; -------------------------------------------------------------------------------------------------------------------

(defn desc [loc]
  (if-not (good? loc)
    "nil"
    (let [node (get-node loc)]
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