(ns plastic.worker.editor.xforms.zipops
  (:refer-clojure :exclude [find remove])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [thread-zip-ops]])
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
(def zip-next (partial zip-utils/zip-next strip-whitespaces-but-keep-linebreaks-policy))
(def zip-prev (partial zip-utils/zip-prev strip-whitespaces-but-keep-linebreaks-policy))

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
; zip ops
;
; each zip-op receives context [loc report], where
;   loc is a zipper before operation, pointing to operation target
;   report is existing report to be updated
;
; and returns updated context [loc report], where
;   loc is a zipper with effective changes after operation
;   report updated with description of new changes
;
; zip ops can be composed, see xforming.cljs

; -------------------------------------------------------------------------------------------------------------------
; movement ops

(defn step-down [[loc report]]
  (if-let [child-loc (zip-down loc)]
    [child-loc report]))

(defn step-up [[loc report]]
  (if-let [new-loc (zip-up loc)]
    [new-loc report]))

(defn step-left [[loc report]]
  (if-let [new-loc (zip-left loc)]
    [new-loc report]))

(defn step-right [[loc report]]
  (if-let [new-loc (zip-right loc)]
    [new-loc report]))

(defn step-next [[loc report]]
  (if-let [new-loc (zip-next loc)]
    [new-loc report]))

(defn step-prev [[loc report]]
  (if-let [new-loc (zip-right loc)]
    [new-loc report]))

(defn move-top [[initial-loc report]]
  (loop [loc initial-loc]
    (let [up-loc (zip-up loc)]
      (if (valid-loc? up-loc)
        (recur up-loc)
        [loc report]))))

(defn find [node-id [loc report]]
  (let [found-loc (findz/find-depth-first loc (partial loc-id? node-id))]
    (if (valid-loc? found-loc)
      [found-loc report])))

(defn lookup [node-id [loc report]]
  (find node-id (move-top [loc report])))

; -------------------------------------------------------------------------------------------------------------------
; editing ops

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

; -------------------------------------------------------------------------------------------------------------------
; composed ops

; thread-zip-ops macro works like ->>, but
;   * it uses vectors instead of lists (to make Cursive happy)
;   * silently bails when nil is result of any operation
;   * does optional logging, see plastic.env.log-zip-ops and plastic.env.log-threaded-zip-ops

(defn commit-node-value [node-id value [loc report]]
  (thread-zip-ops [loc report]
    [lookup node-id]
    [commit value]))

(defn remove-with-pred [pred [loc report]]
  (thread-zip-ops [loc report]
    [remove-by-moving-with-pred identity pred]))

(defn remove-whitespaces-and-newlines-after [[loc report]]
  (thread-zip-ops [loc report]
    [remove-by-moving-with-pred z/right node/whitespace?]))

(defn remove-whitespaces-and-newlines-before [[loc report]]
  (or
    (thread-zip-ops [loc report]
      [step-left]
      [remove-with-pred node/whitespace?]
      [step-right])
    [loc report]))

(defn delete-node [node-id [loc report]]
  (thread-zip-ops [loc report]
    [lookup node-id]
    [remove-whitespaces-and-newlines-before]
    [remove]))

(defn insert-values-after-node [values node-id [loc report]]
  (thread-zip-ops [loc report]
    [lookup node-id]
    [insert-after values]))

(defn insert-values-before-node [values node-id [loc report]]
  (thread-zip-ops [loc report]
    [lookup node-id]
    [insert-before values]))

(defn insert-values-before-first-child-of-node [values node-id [loc report]]
  (thread-zip-ops [loc report]
    [lookup node-id]
    [step-down]
    [insert-before values]))

(defn remove-linebreak-before-node [node-id [loc report]]
  (thread-zip-ops [loc report]
    [lookup node-id]
    [remove-linebreak-before]))

(defn remove-right-siblink-of-node [node-id [loc report]]
  (thread-zip-ops [loc report]
    [lookup node-id]
    [step-right]
    [remove]))

(defn remove-left-siblink-of-node [node-id [loc report]]
  (thread-zip-ops [loc report]
    [lookup node-id]
    [step-left]
    [remove]
    [step-next]))

(defn remove-first-child-of-node [node-id [loc report]]
  (thread-zip-ops [loc report]
    [lookup node-id]
    [step-down]
    [remove]))
