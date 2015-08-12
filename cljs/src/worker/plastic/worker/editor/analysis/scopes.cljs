(ns plastic.worker.editor.layout.analysis.scopes
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.helpers :as helpers]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.token :refer [TokenNode]]
            [clojure.zip :as z]
            [clojure.walk :as walk]
            [plastic.util.zip :as zip-utils]
            [rewrite-clj.zip :as zip]))

(defonce ^:dynamic *scope-id* 0)
(defonce ^:dynamic *scope-locals* nil)

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

(defn collect-node-params [loc max-path]
  (let [subtree-tip-loc (zip-utils/independent-zipper loc)
        all-subtree-locs (take-while zip-utils/valid-loc? (iterate zip-next subtree-tip-loc))
        symbol-nodes (filter #(instance? TokenNode %) (map zip/node all-subtree-locs))]
    (filter-non-args
      (map (fn [node] [node max-path]) symbol-nodes))))

; TODO: here must be proper parsing of destructuring
(defn collect-vector-params [loc]
  (if loc
    (collect-node-params loc (zip-utils/inc-path (zip-utils/loc->path loc)))))

; TODO: here must be proper parsing of destructuring
(defn collect-vector-pairs [loc]
  (if loc
    (let [pairs (partition 2 (filter scope-related? (zip-utils/collect-all-children loc)))]
      (mapcat #(collect-node-params (first %) (zip-utils/inc-path (zip-utils/inc-path (zip-utils/loc->path (second %))))) pairs))))

(defn collect-specified-param [loc num]
  (let [relevant-child-locs (filter scope-related? (zip-utils/collect-all-children loc))]
    (if-let [specified-param-loc (nth relevant-child-locs num nil)]
      (collect-node-params specified-param-loc (zip-utils/inc-path (zip-utils/loc->path specified-param-loc))))))

(defn first-vector [loc]
  (first (filter #(= (zip/tag %) :vector) (zip-utils/collect-all-children loc))))

(defn collect-params [loc opener-type]
  (condp = opener-type
    :params (collect-vector-params (first-vector loc))
    :pairs (collect-vector-pairs (first-vector loc))
    (do
      (assert (number? opener-type))
      (collect-specified-param loc opener-type))))

(defn collect-all-right [loc]
  (take-while zip-utils/valid-loc? (iterate zip-right loc)))

(defn child-locs [loc]
  (collect-all-right (zip-down loc)))

(defn store-locals [scope locals]
  (set! *scope-locals* (assoc *scope-locals* (:id scope) locals))
  scope)

(defn get-locals [scope]
  (get *scope-locals* (:id scope)))

(defn node-scope [loc childs depth]
  (condp = (zip/tag loc)
    :list (if-not (empty? childs)
            (if-let [opener-type (scope-openers (zip/sexpr (first childs)))]
              (store-locals {:id    (next-scope-id!)
                             :depth (inc depth)}
                (collect-params loc opener-type))))
    :fn (store-locals {:id    (next-scope-id!)
                       :depth (inc depth)}
          [[#(re-find #"^%" (zip/string %)) (zip-utils/loc->path loc)]])
    nil))

(defn matching-local? [loc [decl-node effective-marker-path]]
  (if (fn? decl-node)
    (decl-node loc)
    (or
      (identical? (z/node loc) decl-node)
      (and
        (= (node/string (z/node loc)) (node/string decl-node))
        (zip-utils/path<= effective-marker-path (zip-utils/loc->path loc))))))

(defn find-symbol-declaration [loc scope-info]
  (if scope-info
    (let [locals (get-locals (get scope-info :scope))
          matching-locals (filter (partial matching-local? loc) locals)
          hit-count (count matching-locals)
          best-node (first (last matching-locals))]
      (if-not (= hit-count 0)
        (merge (:scope scope-info)
          {:shadows hit-count}
          (if (identical? best-node (z/node loc))
            {:decl? true}))
        (find-symbol-declaration loc (:parent-scope scope-info))))))

(defn resolve-symbol [loc scope-info]
  (if (= (zip/tag loc) :token)
    (if-let [declaration-scope (find-symbol-declaration loc scope-info)]
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
        (conj (analyze-child-scopes new-scope-info) [id (resolve-symbol loc new-scope-info)]))
      (let [old-scope-info (dissoc scope-info :new-scope?)]
        (conj (analyze-child-scopes old-scope-info) [id (resolve-symbol loc old-scope-info)])))))

(defn analyze-scopes [analysis loc]
  (binding [*scope-id* 0
            *scope-locals* {}]
    (let [starting-loc (if-not (scope-related? loc) (zip-next loc) loc)]
      (helpers/deep-merge analysis (analyze-scope nil starting-loc)))))