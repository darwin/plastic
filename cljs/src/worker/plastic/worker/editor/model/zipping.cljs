(ns plastic.worker.editor.model.zipping
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.findz :as findz]
            [rewrite-clj.node :as node]
            [plastic.util.zip :as zip-utils]
            [clojure.zip :as z]))

; operations on parse-tree's zippers

(defn strip-whitespaces-but-keep-linebreaks-policy [loc]
  (let [node (z/node loc)]
    (and (or (node/linebreak? node) (not (node/whitespace? node))))))

(def zip-down (partial zip-utils/zip-down strip-whitespaces-but-keep-linebreaks-policy))
(def zip-right (partial zip-utils/zip-right strip-whitespaces-but-keep-linebreaks-policy))
(def zip-left (partial zip-utils/zip-left strip-whitespaces-but-keep-linebreaks-policy))

; -------------------------------------------------------------------------------------------------------------------

(defn transfer-sticky-attrs [old new]
  (let [sticky-bits (select-keys old [:id])]
    (merge new sticky-bits)))

(defn empty-value? [node]
  (let [text (node/string node)
        sexpr (node/sexpr node)]
    (or
      (and (symbol? sexpr) (empty? text))
      (and (keyword? sexpr) (= ":" text)))))

; -------------------------------------------------------------------------------------------------------------------

(defn commit-value [loc node-id new-node]
  {:pre [(:id new-node)]}
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))]
    (assert (zip-utils/valid-loc? node-loc))
    (if-not (empty-value? new-node)
      (z/edit node-loc (fn [old-node] (transfer-sticky-attrs old-node new-node)))
      (z/remove node-loc))))

(defn delete-whitespaces-and-newlines-after [loc]
  (loop [cur-loc loc]
    (let [possibly-white-space-loc (z/right cur-loc)]
      (if (and
            (zip-utils/valid-loc? possibly-white-space-loc)
            (node/whitespace? (zip/node possibly-white-space-loc)))
        (recur (z/remove possibly-white-space-loc))
        cur-loc))))

(defn delete-whitespaces-and-newlines-before [loc]
  (loop [cur-loc loc]
    (let [possibly-white-space-loc (z/left cur-loc)]
      (if (and
            (zip-utils/valid-loc? possibly-white-space-loc)
            (node/whitespace? (zip/node possibly-white-space-loc)))
        (let [loc-after-removal (z/remove possibly-white-space-loc)
              next-step-loc (z/next loc-after-removal)]
          (recur next-step-loc))
        cur-loc))))

(defn delete-node [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))]
    (assert (zip-utils/valid-loc? node-loc))
    (z/remove (delete-whitespaces-and-newlines-before node-loc))))

(defn insert-values-after [node-id values loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        inserter (fn [loc val] {:pre [(:id val)]} (z/insert-right loc val))]
    (reduce inserter node-loc (reverse values))))

(defn insert-values-before [node-id values loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        inserter (fn [loc val] {:pre [(:id val)]} (z/insert-left loc val))]
    (reduce inserter node-loc values)))

(defn insert-values-before-first-child [node-id values loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        first-child-loc (zip-down node-loc)
        _ (assert (zip-utils/valid-loc? first-child-loc))
        inserter (fn [loc val] {:pre [(:id val)]} (z/insert-left loc val))]
    (reduce inserter first-child-loc values)))

(defn remove-linebreak-before [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        prev-locs (take-while zip-utils/valid-loc? (iterate z/prev node-loc))
        first-linebreak (first (filter #(node/linebreak? (zip/node %)) prev-locs))]
    (if first-linebreak
      (z/remove first-linebreak))))

(defn remove-right-siblink [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        right-loc (zip-right node-loc)]
    (if right-loc
      (loop [loc right-loc]
        (let [new-loc (z/remove loc)]
          (if (= (zip-utils/loc-id new-loc) node-id)
            new-loc
            (recur new-loc)))))))

(defn remove-left-siblink [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        left-loc (zip-left node-loc)]
    (if left-loc
      (z/remove left-loc))))

(defn remove-first-child [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        first-child-loc (zip-down node-loc)]
    (if first-child-loc
      (loop [loc first-child-loc]
        (let [new-loc (z/remove loc)]
          (if (= (zip-utils/loc-id new-loc) node-id)
            new-loc
            (recur new-loc)))))))
