(ns plastic.worker.editor.model.zipops
  (:refer-clojure :exclude [find remove])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.findz :as findz]
            [rewrite-clj.node :as node]
            [plastic.util.zip :as zip-utils :refer [loc-id loc-id? valid-loc?]]
            [clojure.zip :as z]
            [plastic.worker.editor.model.report :as report]))

; operations with parse-tree's zippers

(defn strip-whitespaces-but-keep-linebreaks-policy [loc]
  (let [node (z/node loc)]
    (and (or (node/linebreak? node) (not (node/whitespace? node))))))

(def zip-up (partial zip-utils/zip-up strip-whitespaces-but-keep-linebreaks-policy))
(def zip-down (partial zip-utils/zip-down strip-whitespaces-but-keep-linebreaks-policy))
(def zip-right (partial zip-utils/zip-right strip-whitespaces-but-keep-linebreaks-policy))
(def zip-left (partial zip-utils/zip-left strip-whitespaces-but-keep-linebreaks-policy))

; -------------------------------------------------------------------------------------------------------------------

(defn transfer-sticky-attrs [old-node new-node]
  (let [sticky-bits (select-keys old-node [:id])]
    (merge new-node sticky-bits)))

(defn empty-value? [node]
  (let [text (node/string node)
        sexpr (node/sexpr node)]
    (or
      (and (symbol? sexpr) (empty? text))
      (and (keyword? sexpr) (= ":" text)))))

; -------------------------------------------------------------------------------------------------------------------

(defn report-modified [report loc]
  (report/merge report (report/make-modified (loc-id loc))))

(defn report-added [report loc]
  (report/merge report (report/make-added (loc-id loc))))

(defn report-removed [report loc]
  (report/merge report (report/make-removed (loc-id loc))))

(defn report-moved [report loc]
  (report/merge report (report/make-moved (loc-id loc))))

; -------------------------------------------------------------------------------------------------------------------
; elemental zip ops
;
; each zip-op receives context [loc report], where
;   loc is a zipper before operation
;   report to be updated
;
; and returns updated context [loc report], where
;   loc is a zipper with effective changes after operation
;   report updated with description of new changes

; movement zip ops

(defn find [node-id [loc report]]
  (let [found-loc (findz/find-depth-first loc (partial loc-id? node-id))]
    (assert (valid-loc? found-loc))
    [found-loc report]))

(defn step-down [[loc report]]
  (let [child-loc (zip-down loc)]
    (assert (valid-loc? child-loc)
      [child-loc report])))

(defn step-left [[loc report]]
  [(zip-left loc) report])

(defn step-right [[loc report]]
  [(zip-right loc) report])

(defn step-up [[loc report]]
  [(zip-up loc) report])

; editing zip ops

(defn remove [[loc report]]
  [(z/remove loc) (report-removed report loc)])

(defn edit [new-node [loc report]]
  [(z/edit loc (fn [old-node] (transfer-sticky-attrs old-node new-node))) (report-modified report loc)])

(defn commit [new-node [loc report]]
  (if-not (empty-value? new-node)
    (edit new-node [loc report])
    (remove [loc report])))

(defn remove-by-moving-with-pred [movement pred [initial-loc initial-report]]
  (loop [[loc report] [initial-loc initial-report]]
    (let [next-loc (movement loc)]
      (if (and (valid-loc? next-loc) (pred (zip/node next-loc)))
        (recur (remove [next-loc report]))
        [loc report]))))

(defn remove-whitespaces-and-newlines-after [[loc report]]
  (remove-by-moving-with-pred z/right node/whitespace? [loc report]))

(defn remove-whitespaces-and-newlines-before [[loc report]]
  (remove-by-moving-with-pred z/left node/whitespace? [loc report]))

(defn insert-after [values [initial-loc initial-report]]
  (let [inserter (fn [[loc report] val]
                   (let [inserted-loc (z/insert-right loc val)]
                     [inserted-loc (report-added report inserted-loc)]))]
    (reduce inserter [initial-loc initial-report] (reverse values))))

(defn insert-before [values [initial-loc initial-report]]
  (let [inserter (fn [[loc report] val]
                   (let [inserted-loc (z/insert-left loc val)]
                     [inserted-loc (report-added report inserted-loc)]))]
    (reduce inserter [initial-loc initial-report] values)))

(defn remove-linebreak-before [[loc report]]
  (let [prev-locs (take-while valid-loc? (iterate z/prev loc))
        ln-loc (first (filter #(node/linebreak? (zip/node %)) prev-locs))]
    (if (valid-loc? ln-loc)
      (remove [ln-loc report])
      [loc report])))
