(ns plastic.worker.editor.xforms.zipops
  (:refer-clojure :exclude [find remove])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [thread-zip-ops]]
                   [plastic.common :refer [process]])
  (:require [plastic.worker.editor.model.report :as report]
            [meld.zip :as zip]))

; operations with parse-tree's zippers

; -------------------------------------------------------------------------------------------------------------------

(defn transfer-sticky-attrs [old-node new-node]
  (let [sticky-bits (select-keys old-node [:id])]
    (merge new-node sticky-bits)))

; -------------------------------------------------------------------------------------------------------------------

(defn report* [f report loc]
  (report/merge report (f (zip/get-id loc))))

(def report-modified (partial report* report/make-modified))
(def report-added (partial report* report/make-added))
(def report-removed (partial report* report/make-removed))
(def report-moved (partial report* report/make-moved))

(defn report*list [f report locs]
  (report/merge report (f (map zip/get-id locs))))

(def report-modified-list (partial report*list report/make-modified-list))
(def report-added-list (partial report*list report/make-added-list))
(def report-removed-list (partial report*list report/make-removed-list))
(def report-moved-list (partial report*list report/make-moved-list))

(defn report*nodes [f report nodes]
  (report/merge report (f (map :id nodes))))

(def report-modified-nodes (partial report*nodes report/make-modified-list))
(def report-added-nodes (partial report*nodes report/make-added-list))
(def report-removed-nodes (partial report*nodes report/make-removed-list))
(def report-moved-nodes (partial report*nodes report/make-moved-list))

; -------------------------------------------------------------------------------------------------------------------
; zip ops
;
; each zip-op receives (optional) parameters and context [loc report], where
;   loc is a zipper before operation, pointing to operation target
;   report is existing report to be updated
;
; and returns updated context [loc report], where
;   loc is a zipper with effective changes after operation
;   report updated with description of new changes
;
; zip ops can be composed

; -------------------------------------------------------------------------------------------------------------------
; movement ops

(defn step-down [[loc report]]
  (if-let [child-loc (zip/down loc)]
    [child-loc report]))

(defn step-up [[loc report]]
  (if-let [new-loc (zip/up loc)]
    [new-loc report]))

(defn step-left [[loc report]]
  (if-let [new-loc (zip/left loc)]
    [new-loc report]))

(defn step-right [[loc report]]
  (if-let [new-loc (zip/right loc)]
    [new-loc report]))

(defn step-next [[loc report]]
  (if-let [new-loc (zip/next loc)]
    [new-loc report]))

(defn step-prev [[loc report]]
  (if-let [new-loc (zip/prev loc)]
    [new-loc report]))

(defn move-top [[loc report]]
  (if-let [new-loc (zip/top loc)]
    [new-loc report]))

(defn find [node-id [loc report]]
  (if-let [found-loc (zip/find loc node-id)]
    [found-loc report]))

; -------------------------------------------------------------------------------------------------------------------
; editing ops

(defn remove [[loc report]]
  [(zip/remove loc) (report-removed report loc)])

(defn edit [new-node [loc report]]
  [(zip/edit loc (fn [old-node] (transfer-sticky-attrs old-node new-node))) (report-modified report loc)])

(defn commit [new-node [loc report]]
  (if new-node
    (edit new-node [loc report])
    (remove [loc report])))

(defn remove-by-moving-with-pred [movement pred [initial-loc initial-report]]
  (loop [[loc report] [initial-loc initial-report]]
    (let [next-loc (movement loc)]
      (if (and (zip/good? next-loc) (pred next-loc))
        (recur (remove [next-loc report]))
        [loc report]))))

(defn insert-after [values [loc report]]
  [(zip/insert-rights loc values) (report-added-nodes report values)])

(defn insert-before [values [loc report]]
  [(zip/insert-lefts loc values) (report-added-nodes report values)])

(defn remove-linebreak-before [[loc report]]
  (let [prev-locs (take-while zip/good? (iterate zip/prev loc))
        ln-loc (first (filter zip/linebreak? prev-locs))]
    (if (zip/good? ln-loc)
      (remove [ln-loc report])
      [loc report])))

#_(defn splice [[loc report]]
  (let [children (z/children loc)]
    [(editz/splice loc) (report-moved-nodes report children)]))

; -------------------------------------------------------------------------------------------------------------------
; composed ops

; thread-zip-ops macro works like ->>, but
;   * it uses vectors instead of lists (to make Cursive happy)
;   * silently bails when nil is result of any operation
;   * does optional logging, see plastic.env.log-zip-ops and plastic.env.log-threaded-zip-ops

(defn commit-node-value [node-id value [loc report]]
  (thread-zip-ops [loc report]
    [find node-id]
    [commit value]))

(defn remove-with-pred [pred [loc report]]
  (thread-zip-ops [loc report]
    [remove-by-moving-with-pred identity pred]))

(defn remove-linebreaks-after [[loc report]]
  (thread-zip-ops [loc report]
    [remove-by-moving-with-pred zip/right zip/linebreak?]))

(defn remove-linebreaks-before [[loc report]]
  (or
    (thread-zip-ops [loc report]
      [step-left]
      [remove-with-pred zip/linebreak?]
      [step-right])
    [loc report]))

(defn delete-node [node-id [loc report]]
  (thread-zip-ops [loc report]
    [find node-id]
    [remove-linebreaks-before]
    [remove]
    [remove-linebreaks-before]))

(defn insert-values-after-node [values node-id [loc report]]
  (thread-zip-ops [loc report]
    [find node-id]
    [insert-after values]))

(defn insert-values-before-node [values node-id [loc report]]
  (thread-zip-ops [loc report]
    [find node-id]
    [insert-before values]))

(defn insert-values-before-first-child-of-node [values node-id [loc report]]
  (thread-zip-ops [loc report]
    [find node-id]
    [step-down]
    [insert-before values]))

(defn remove-linebreak-before-node [node-id [loc report]]
  (thread-zip-ops [loc report]
    [find node-id]
    [remove-linebreak-before]))

(defn remove-right-siblink-of-node [node-id [loc report]]
  (thread-zip-ops [loc report]
    [find node-id]
    [step-right]
    [remove]))

(defn remove-left-siblink-of-node [node-id [loc report]]
  (thread-zip-ops [loc report]
    [find node-id]
    [step-left]
    [remove]
    [step-next]))

(defn remove-first-child-of-node [node-id [loc report]]
  (thread-zip-ops [loc report]
    [find node-id]
    [step-down]
    [remove]))

(defn splice-node [node-id [loc report]]
  (error "implement splice-node")
  [loc report]
  #_(thread-zip-ops [loc report]
    [find node-id]
    [splice]))
