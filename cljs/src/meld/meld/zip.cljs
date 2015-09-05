(ns meld.zip
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [clojure.zip :as z]))

(defn node-replace-children [node new-children]
  (assoc node :children new-children))

(defn node-inner? [node]
  (keyword-identical? (:type node) :compound))

(defn node-children [node]
  (seq (:children node)))

(defn make-zipper [root]
  (z/zipper
    node-inner?
    node-children
    node-replace-children
    root))

; -------------------------------------------------------------------------------------------------------------------

(defn top [loc]
  (->> loc
    (iterate z/up)
    (take-while identity)
    last))

(defn matching-end-loc? [end loc]
  (= (:end (z/node loc)) end))

(defn insert-rights [loc nodes]
  (let [* (fn [loc node] (z/insert-right loc node))]
    (reduce * loc (reverse nodes))))

(defn insert-childs [loc nodes]
  (let [* (fn [loc node] (z/insert-child loc node))]
    (reduce * loc (reverse nodes))))

(defn whitespace-loc? [loc]
  (#{:whitespace} (:type (z/node loc))))

(defn is-compound? [loc]
  (if (z/end? loc)
    false
    (let [node (z/node loc)]
      (#{:compound} (:type node)))))

; -------------------------------------------------------------------------------------------------------------------

(defn next-token [loc]
  (let [next-loc (z/next loc)]
    (first (drop-while is-compound? (iterate z/next next-loc)))))
