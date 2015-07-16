(ns plastic.util.zip
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [reagent.ratom]
            [clojure.zip :as z]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]))

(defn valid-loc? [loc]
  (not (or (nil? loc) (z/end? loc) (zip/end? loc))))        ; why is zip/end? doing it differently than z/end?

(defn make-zipper* [node]
  (z/zipper
    node/inner?
    (comp seq node/children)
    node/replace-children
    node))

(defn make-zipper [node]
  (if (= (node/tag node) :forms)
    (make-zipper* node)
    (recur (node/forms-node [node]))))

(defn independent-zipper [loc]
  (make-zipper (z/node loc)))

; perform the given movement while the given policy predicate returns true
(defn skip [movement policy loc]
  (first
    (drop-while #(and (valid-loc? %) (not (policy %)))
      (iterate movement loc))))

(defn zip-right [policy loc]
  (some->> loc z/right (skip z/right policy)))

(defn zip-left [policy loc]
  (some->> loc z/left (skip z/left policy)))

(defn zip-down [policy loc]
  (some->> loc z/down (skip z/right policy)))

(defn zip-up [policy loc]
  (some->> loc z/up (skip z/left policy)))

(defn zip-next [policy loc]
  (some->> loc z/next (skip z/right policy)))

(defn leaf-nodes [policy loc]
  (filter (complement z/branch?)                            ; filter only non-branch nodes
    (take-while valid-loc?                                  ; take until the :end
      (iterate (partial zip-next policy) loc))))

(defn ancestor-count [policy loc]
  (dec (count (take-while valid-loc? (iterate (partial zip-up policy) loc)))))

(defn left-siblings-count [loc]
  (count (take-while z/left (iterate z/left loc))))

(defn loc->path [loc]
  (let [parent-loc (z/up loc)
        pos (left-siblings-count loc)]
    (if parent-loc
      (conj (loc->path parent-loc) pos)
      [])))

(defn path->loc [path loc]
  (if (and path (valid-loc? loc))
    (if (empty? path)
      loc
      (let [down-loc (z/down loc)
            child-loc (nth (iterate z/right down-loc) (first path))]
        (if (valid-loc? child-loc)
          (recur (rest path) child-loc))))))

(defn collect-all-right [loc]
  (take-while (complement zip/end?) (iterate zip/right loc)))

(defn collect-all-parents [loc]
  (take-while valid-loc? (iterate z/up loc)))

(defn collect-all-children [loc]
  (collect-all-right (z/down loc)))

(defn children-locs [loc]
  (take-while valid-loc? (iterate z/right (z/down loc))))

(defn zip-seq [loc]
  (tree-seq z/branch? children-locs loc))

(defn zip-node-seq [loc]
  (map z/node (zip-seq loc)))

(defn loc-id [loc]
  (:id (z/node loc)))

(defn loc-id? [id loc]
  (= (loc-id loc) id))

