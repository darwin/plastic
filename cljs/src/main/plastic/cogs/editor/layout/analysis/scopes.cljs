(ns plastic.cogs.editor.layout.analysis.scopes
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.helpers :as helpers]
            [rewrite-clj.node :as node]
            [clojure.zip :as z]
            [plastic.util.zip :as zip-utils]
            [rewrite-clj.zip :as zip]))

(defonce ^:dynamic *scope-id* 0)

(defn next-scope-id! []
  (set! *scope-id* (inc *scope-id*))
  *scope-id*)

(def scope-openers
  {'defn      :params
   'defn-     :params
   'fn        :params
   'defmethod :params
   'let       :pairs
   'if-let    :pairs
   'when-let  :pairs
   'for       :pairs
   'loop      :pairs})

(defn scope-related? [loc]
  (let [node (z/node loc)]
    (not (or (node/whitespace? node) (node/comment? node)))))

(def zip-up (partial zip-utils/zip-up scope-related?))
(def zip-down (partial zip-utils/zip-down scope-related?))
(def zip-left (partial zip-utils/zip-left scope-related?))
(def zip-right (partial zip-utils/zip-right scope-related?))
(def zip-next (partial zip-utils/zip-next scope-related?))

(defn filter-non-args [arg-nodes]
  (let [arg? (fn [[node _]]
               (let [s (node/string node)]
                 (not (or (= s "&") (= (first s) "_")))))]
    (filter arg? arg-nodes)))

(defn get-max-id [node current]
  (if (node/inner? node)
    (max current (:id node) (apply max (map :id (node/children node))))
    (max current (:id node))))

; TODO: here must be proper parsing of destructuring
(defn collect-vector-params [node]
  (filter-non-args (map (fn [node] [node (:id node)]) (filter (complement node/whitespace?) (node/children node)))))

; TODO: here must be proper parsing of destructuring
(defn collect-vector-pairs [node]
  (filter-non-args
    (let [pairs (partition 2 (filter (complement node/whitespace?) (node/children node)))]
      (for [pair pairs]
        [(first pair) (get-max-id (second pair) 0)]))))

(defn collect-params [node opener-type]
  (if-let [first-vector (first (filter #(= (node/tag %) :vector) (node/children node)))]
    (condp = opener-type
      :params (collect-vector-params first-vector)
      :pairs (collect-vector-pairs first-vector))))

(defn collect-all-right [loc]
  (take-while zip-utils/valid-loc? (iterate zip-right loc)))

(defn child-locs [loc]
  (collect-all-right (zip-down loc)))

(defn node-scope [loc childs depth]
  (condp = (zip/tag loc)
    :list (if-not (empty? childs)
            (if-let [opener-type (scope-openers (zip/sexpr (first childs)))]
              {:id     (next-scope-id!)
               :depth  (inc depth)
               :locals (collect-params (z/node loc) opener-type)}))
    :fn {:id     (next-scope-id!)
         :depth  (inc depth)
         :locals [[#(re-find #"^%" (node/string %)) (:id (z/node loc))]]}
    nil))

(defn analyze-scope [scope-info loc]
  (let [id (:id (z/node loc))
        childs (child-locs loc)
        depth (get-in scope-info [:scope :depth])
        analyze-child-scopes (fn [scope] (into {} (map (partial analyze-scope scope) childs)))]
    (if-let [new-scope (node-scope loc childs depth)]
      (let [new-scope-info {:scope new-scope :parent-scope scope-info}]
        (conj (analyze-child-scopes new-scope-info) [id new-scope-info]))
      (conj (analyze-child-scopes scope-info) [id scope-info]))))

(defn analyze-scopes [loc analysis]
  (binding [*scope-id* 0]
    (let [starting-loc (if-not (scope-related? loc) (zip-next loc) loc)
          initial-scope {:scope nil :parent-scope nil}]
      (helpers/deep-merge analysis (analyze-scope initial-scope starting-loc)))))