(ns quark.cogs.editor.utils
  (:require [rewrite-clj.zip :as rzip]
            [rewrite-clj.node :as rnode]
            [rewrite-clj.zip.whitespace :as ws]
            [quark.cogs.editor.analyzer :refer [analyze-full]]
            [clojure.zip :as z])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

; terminology
;
; rewrite-clj gives us tree of nodes, we prefix them with "r" e.g. rnode
;
; rewrite-clj can give us zippers and sexprs, sexprs are typically called "forms"
; we can convert between zippers and trees seamlessly, trees can be converted to sexprs, but not back
;
; zipper stuff is prefixed with "z", e.g. zloc is a zipper (location)
;
; we also introduce notion of paths, path is a description how to reach particular node from the root of the tree

(defn make-zipper* [rnode]
  (z/zipper
    rnode/inner?
    (comp seq rnode/children)
    rnode/replace-children
    rnode))

(defn make-zipper [rnode]
  (if (= (rnode/tag rnode) :forms)
    (let [top (make-zipper* rnode)]
      (or (-> top rzip/down ws/skip-whitespace) top))
    (recur (rnode/forms-node [rnode]))))

; interesting nodes are non-whitespace nodes and new lines
(defn node-interesting? [rnode]
  (or (rnode/linebreak? rnode) (not (rnode/whitespace? rnode))))

(def node-not-interesting? (complement node-interesting?))

(defn not-interesting? [zloc]
  (node-not-interesting? (rzip/node zloc)))

; perform the given movement while the given predicate returns true
(defn skip [f p? zloc]
  (first
    (drop-while #(if (or (z/end? %) (nil? %)) false (p? %))
      (iterate f zloc))))

(defn skip-not-interesting [f zloc]
  (skip f not-interesting? zloc))

(defn skip-not-interesting-by-moving-left [zloc]
  (skip-not-interesting z/left zloc))

(defn skip-not-interesting-by-moving-right [zloc]
  (skip-not-interesting z/right zloc))

(defn move-right [zloc]
  (some-> zloc z/right skip-not-interesting-by-moving-right))

(defn move-left [zloc]
  (some-> zloc z/left skip-not-interesting-by-moving-left))

(defn move-down [zloc]
  (some-> zloc z/down skip-not-interesting-by-moving-right))

(defn move-up [zloc]
  (some-> zloc z/up skip-not-interesting-by-moving-left))

(defn move-next [zloc]
  (some-> zloc z/next skip-not-interesting-by-moving-right))

(defn leaf-nodes [zloc]
  (filter (complement z/branch?)                            ; filter only non-branch nodes
    (take-while (complement z/end?)                         ; take until the :end
      (iterate move-next zloc))))

(defn ancestor-count [zloc]
  (dec (count (take-while move-up (iterate move-up zloc)))))

(defn left-sibling-count [zloc]
  (count (take-while move-left (iterate move-left zloc))))

(defn root? [zloc]
  (nil? (move-up zloc)))

(defn make-rpath [zloc]
  (let [zparent (move-up zloc)]
    (if (root? zparent)
      []
      (conj (make-rpath zparent) (left-sibling-count zloc)))))

(defn collect-all-right [zloc]
  (take-while (complement rzip/end?) (iterate rzip/right zloc)))

