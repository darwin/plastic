(ns plastic.util.zip
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [reagent.ratom]
            [clojure.zip :as z]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]))

(defn valid-loc? [loc]
  (not (or (nil? loc) (z/end? loc))))

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

(defn inc-path [path]
  (update path (dec (count path)) inc))

(defn path->loc [path loc]
  (if (and path (valid-loc? loc))
    (if (empty? path)
      loc
      (let [down-loc (z/down loc)
            child-loc (nth (iterate z/right down-loc) (first path))]
        (if (valid-loc? child-loc)
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

(defn collect-all-right [loc]
  (take-while valid-loc? (iterate zip/right loc)))

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

