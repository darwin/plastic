(ns plastic.cogs.editor.layout.analysis.scopes
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.helpers :as helpers]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.token :refer [TokenNode]]
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
   'loop      :pairs
   'catch     2})

(defn scope-related? [loc]
  (let [node (z/node loc)]
    (not (or (node/whitespace? node) (node/comment? node)))))

(def zip-down (partial zip-utils/zip-down scope-related?))
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

(defn collect-node-params [node max-id]
  (let [loc (zip-utils/make-zipper node)
        all-locs (take-while zip-utils/valid-loc? (iterate zip-next loc))
        symbol-nodes (filter #(instance? TokenNode %) (map zip/node all-locs))]
    (filter-non-args
      (map (fn [node] [node max-id]) symbol-nodes))))

; TODO: here must be proper parsing of destructuring
(defn collect-vector-params [node]
  (if node
    (collect-node-params node (:id node))))

; TODO: here must be proper parsing of destructuring
(defn collect-vector-pairs [node]
  (if node
    (let [pairs (partition 2 (filter (complement node/whitespace?) (node/children node)))]
      (mapcat #(collect-node-params (first %) (get-max-id (second %) 0)) pairs))))

(defn collect-specified-param [node num]
  (let [relevant-children (filter (complement node/whitespace?) (node/children node))]
    (if-let [specified-param-node (nth relevant-children num nil)]
      (collect-node-params specified-param-node (:id specified-param-node)))))

(defn first-vector [node]
  (first (filter #(= (node/tag %) :vector) (node/children node))))

(defn collect-params [node opener-type]
  (condp = opener-type
    :params (collect-vector-params (first-vector node))
    :pairs (collect-vector-pairs (first-vector node))
    (do
      (assert (number? opener-type))
      (collect-specified-param node opener-type))))

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

(defn matching-local? [node [decl-node after-id]]
  (if (fn? decl-node)
    (decl-node node)
    (or
      (identical? node decl-node)
      (and
        (> (:id node) after-id)
        (= (node/string node) (node/string decl-node))))))

(defn find-symbol-declaration [node scope-info]
  (if scope-info
    (let [locals (get-in scope-info [:scope :locals])
          matching-locals (filter (partial matching-local? node) locals)
          hit-count (count matching-locals)
          best-node (first (last matching-locals))]
      (if-not (= hit-count 0)
        (merge (:scope scope-info)
          {:shadows hit-count}
          (if (identical? best-node node)
            {:decl? true}))
        (find-symbol-declaration node (:parent-scope scope-info))))))

(defn resolve-symbol [node scope-info]
  (if (= (node/tag node) :token)
    (if-let [declaration-scope (find-symbol-declaration node scope-info)]
      (assoc scope-info :decl-scope declaration-scope)
      scope-info)
    scope-info))

(defn analyze-scope [scope-info loc]
  (let [node (z/node loc)
        id (:id node)
        childs (child-locs loc)
        depth (get-in scope-info [:scope :depth])
        analyze-child-scopes (fn [scope] (into {} (map (partial analyze-scope scope) childs)))]
    (if-let [new-scope (node-scope loc childs depth)]
      (let [new-scope-info {:new-scope? true :scope new-scope :parent-scope scope-info}]
        (conj (analyze-child-scopes new-scope-info) [id (resolve-symbol node new-scope-info)]))
      (conj (analyze-child-scopes (dissoc scope-info :new-scope?)) [id (resolve-symbol node scope-info)]))))

(defn analyze-scopes [analysis loc]
  (binding [*scope-id* 0]
    (let [starting-loc (if-not (scope-related? loc) (zip-next loc) loc)]
      (helpers/deep-merge analysis (analyze-scope nil starting-loc)))))