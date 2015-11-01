(ns meld.core
  (:refer-clojure :exclude [descendants ancestors])
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [meld.node :as node]
            [meld.util :refer [update!]]))

; -------------------------------------------------------------------------------------------------------------------
;
; introducing the 'meld' library
;
;   https://www.youtube.com/watch?v=lfZAuDcWmc0
;
; This is going to be Plastic's parse-tree library and replace rewrite-cljs.
;
; rewrite-cljs is a great library and helped bootstrap initial Plastic development.
;
; kudos @rundis and @xsc
;
; The main reason is that I want Plastic parser to use recent tools.reader without modifications.
; In the future I will use boostrapped clojurescript to analyze and compile edited code.
; Using identical reader will allow to cross-match Plastic tokens to cljs analyzer ones
; (both will use the same parse-tree data) and later will hopefully allow to pass edited sexprs
; from Plastic directly to cljs analyzer, without going through full text conversion and
; full reader pass after each change.
;
; The secondary reason is that I wanted to move some responsibilities down to rewrite-cljs
; and remove some complexity on Plastic level. This required better understanding of
; rewrite-cljs and changing some original design assumptions. This would lead to a divergent fork.
;
; Some things I wanted to do:
;
; 1. I want linebreaks as individual tokens, rewrite-cljs glues multiple consequent linebreaks together
; 2. I want linebreaks, rewrite-cljs treats linebreaks as whitespace in zip operations by default
; 3. I want to stitch multiple aligned comments as single comment tokens
; 4. I want to glue whitespace token with following non-whitespace token, effectively attaching it for editing ops
; 5. I prefer simple maps without protocols
; 6. I need efficient addressing of nodes by id
; 7. I will need my own whitespace normalization pass when producing final text file
; 8. I want to implement my own effective zipping library on top
;
; I expect Meld to overtake some responsibilities for low-level editing operations.
;
; -------------------------------------------------------------------------------------------------------------------

(def ^:const initial-next-id 1)                                                                                       ; 1-based indexing plays well when meld is empty

(declare get-next-node-id)

(defn sanity-check [meld]
  (assert (> (get-next-node-id meld) (or (apply max (keys meld)) 0)) (pr-str (meta meld)))
  meld)

(defn get-root-node-id [meld]
  {:pre [meld]}
  (::root-node-id (meta meld)))

(defn set-root-node-id [meld node-id]
  {:pre [meld]}
  (vary-meta meld assoc ::root-node-id node-id))

(defn get-next-node-id [meld]
  {:pre [meld]}
  (::next-node-id (meta meld)))

(defn set-next-node-id [meld node-id]
  {:pre [meld]}
  (sanity-check (vary-meta meld assoc ::next-node-id node-id)))

(defn get-node [meld id]
  {:pre [meld]}
  (get meld id))

(defn add-node [meld node]
  {:pre [meld]}
  (let [node-id (node/get-id node)]
    (assert node-id)
    (assert (not (get node-id (keys meld))))
    (assert (> (get-next-node-id meld) node-id))
    (assoc meld node-id node)))

(defn remove-node [meld node-id]
  {:pre [meld]}
  (assert node-id)
  (assert (get node-id (keys meld)))
  (dissoc meld node-id))

(defn get-root-node [meld]
  {:pre [meld]}
  (if-let [root-node-id (get-root-node-id meld)]
    (get-node meld root-node-id)))

(defn get-source [meld]
  {:pre [meld]}
  (node/get-source (get-root-node meld)))

(defn nodes-count [meld]
  {:pre [meld]}
  (count (keys meld)))

(defn make
  ([base root-id] (make base root-id initial-next-id))
  ([base root-id next-id]
   (-> {}
     (into base)
     (set-root-node-id root-id)
     (set-next-node-id next-id))))

; -------------------------------------------------------------------------------------------------------------------

(defn descendants [meld id]
  (let [node (get meld id)
        children (if (node/compound? node) (node/get-children node))]
    (if (seq children)
      (concat children (apply concat (map (partial descendants meld) children)))
      (list))))

(defn all-ancestor-nodes [meld start-id]
  (loop [id start-id
         res []]
    (let [node (get-node meld id)]
      (assert node)
      (if-let [parent-id (node/get-parent node)]
        (recur parent-id (conj res node))
        res))))

; -------------------------------------------------------------------------------------------------------------------

(defn get-compound-metrics [meld node]
  {:pre [node
         (node/compound? node)]}
  (let [children (node/get-children node)]
    (if (empty? children)
      [0 0]
      (let [first-child (get-node meld (first children))
            last-child (get-node meld (last children))
            left-size (- (node/get-range-start first-child) (node/get-start node))
            right-size (- (node/get-range-end node) (node/get-end last-child))]
        [left-size right-size]))))

; -------------------------------------------------------------------------------------------------------------------

(defn ancestors* [meld id]
  (let [node (get-node meld id)]
    (if-let [parent-id (node/get-parent node)]
      (conj (ancestors* meld parent-id) id)
      (list id))))

(defn ancestors [meld id]
  (rest (ancestors* meld id)))

; -------------------------------------------------------------------------------------------------------------------

; tree is a convenience structure for building node tree to be later merged into meld
; a tree is `[node seq-of-children-trees]` or just `node` if leaf
; flatten-tree-into-meld does the work of
;   flattening this nested structure into a meld and
;   assigning parent/children links between nodes
;
; it does not assign parent to the root node of the tree, you have to link it yourself after merging
;   see some flatten-tree-into-meld usages in meld.zip namespace

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

(defn flatten-tree-into-meld& [meld& tree first-id]
  (let [[node-without-id children] (destructure-tree tree)
        node (node/set-id node-without-id first-id)
        * (fn [[meld& current-id ids] child-tree]
            (let [[flatten-meld& next-id] (flatten-tree-into-meld& meld& child-tree current-id)
                  new-meld& (update! flatten-meld& current-id node/set-parent first-id)]
              [new-meld& next-id (conj ids current-id)]))
        [flattened-meld& next-id child-ids] (reduce * [meld& (inc first-id) []] children)
        node-with-children (if (node/compound? node) (node/set-children node child-ids) node)]
    [(assoc! flattened-meld& first-id node-with-children) next-id]))

(defn flatten-tree [tree]
  (let [first-id initial-next-id
        [base& next-id] (flatten-tree-into-meld& (transient {}) tree first-id)]
    (make (persistent! base&) first-id next-id)))

; this is a low-level operation, you have to properly link to the parent of root tree node
(defn merge-tree [meld tree]
  (let [flattened-tree-meld (flatten-tree tree)]
    (merge meld flattened-tree-meld)))