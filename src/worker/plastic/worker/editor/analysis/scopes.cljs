(ns plastic.worker.editor.analysis.scopes
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.helpers :as helpers]
            [meld.zip :as zip]
            [meld.node :as node]))

; -------------------------------------------------------------------------------------------------------------------

(defonce ^:dynamic *scope-id* 0)
(defonce ^:dynamic *scope-locals* nil)
(defonce ^:dynamic *related-rings* nil)

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
  (not (or (zip/comment? loc) (zip/linebreak? loc))))

(defn filter-non-args [arg-nodes]
  (let [arg? (fn [node]
               (let [s (node/get-source node)]
                 (not (or (= s "&") (= s "_")))))]
    (filter arg? arg-nodes)))

(defn collect-node-params [loc max-path]
  (->> loc
    (zip/descendant-locs)
    (map zip/get-node)
    (filter node/symbol?)
    (filter-non-args)
    (map (fn [node] [node max-path]))))

; TODO: here must be proper parsing of destructuring
(defn collect-vector-params [loc]
  (if loc
    (collect-node-params loc (zip/inc-path (zip/loc->path loc)))))

; TODO: here must be proper parsing of destructuring
(defn collect-vector-pairs [loc]
  (if (zip/good? loc)
    (let [pairs (partition 2 (filter scope-related? (zip/child-locs loc)))
          inc-path zip/inc-path
          loc->path zip/loc->path]
      (mapcat #(collect-node-params (first %) (inc-path (inc-path (loc->path (second %))))) pairs))))

(defn collect-specified-param [loc num]
  (let [relevant-child-locs (filter scope-related? (zip/child-locs loc))]
    (if-let [specified-param-loc (nth relevant-child-locs num nil)]
      (collect-node-params specified-param-loc (zip/inc-path (zip/loc->path specified-param-loc))))))

(defn first-vector [loc]
  (first (filter zip/vector? (zip/child-locs loc))))

(defn collect-params [loc opener-type]
  (case opener-type
    :params (collect-vector-params (first-vector loc))
    :pairs (collect-vector-pairs (first-vector loc))
    (do
      (assert (number? opener-type))
      (collect-specified-param loc opener-type))))

(defn store-locals [scope locals]
  (set! *scope-locals* (assoc *scope-locals* (:id scope) locals))
  scope)

(defn get-locals [scope]
  (get *scope-locals* (:id scope)))

(defn node-scope [loc childs depth]
  (case (zip/get-tag loc)
    :list (if-not (empty? childs)
            (if-let [opener-type (scope-openers (zip/get-sexpr (first childs)))]
              (store-locals {:id    (next-scope-id!)
                             :depth (inc depth)}
                (collect-params loc opener-type))))
    :fn (store-locals {:id    (next-scope-id!)
                       :depth (inc depth)}
          [[#(re-find #"^%" (zip/get-source %)) (zip/loc->path loc)]])
    nil))

(defn matching-local? [loc [decl-node effective-marker-path]]
  (if (fn? decl-node)
    (decl-node loc)
    (or
      (identical? (zip/get-node loc) decl-node)
      (and
        (= (zip/get-source loc) (node/get-source decl-node))
        (zip/path<= effective-marker-path (zip/loc->path loc))))))

(defn find-symbol-declaration [loc scope-info]
  (if scope-info
    (let [locals (get-locals (get scope-info :scope))
          matching-locals (filter (partial matching-local? loc) locals)
          hit-count (count matching-locals)
          best-node (first (last matching-locals))]
      (if best-node
        (set! *related-rings* (update *related-rings* (:id best-node) (fn [node-ids]
                                                                        (let [new-id (zip/get-id loc)]
                                                                          (if node-ids
                                                                            (conj node-ids new-id)
                                                                            [new-id]))))))
      (if-not (= hit-count 0)
        (merge (:scope scope-info)
          {:shadows hit-count}
          (if (identical? best-node (zip/get-node loc))
            {:decl? true}))
        (find-symbol-declaration loc (:parent-scope scope-info))))))

(defn resolve-symbol [loc scope-info]
  (or
    (if-not (zip/compound? loc)
      (if-let [declaration-scope (find-symbol-declaration loc scope-info)]
        (assoc scope-info :decl-scope declaration-scope)))
    scope-info))

(defn analyze-scope [scope-info loc]
  (let [id (zip/get-id loc)
        child-locs (zip/child-locs loc)
        depth (get-in scope-info [:scope :depth])
        analyze-child-scopes (fn [scope] (into {} (map (partial analyze-scope scope) child-locs)))]
    (if-let [new-scope (node-scope loc child-locs depth)]
      (let [new-scope-info {:new-scope? true :scope new-scope :parent-scope scope-info}]
        (conj (analyze-child-scopes new-scope-info) [id (resolve-symbol loc new-scope-info)]))
      (let [old-scope-info (dissoc scope-info :new-scope?)]
        (conj (analyze-child-scopes old-scope-info) [id (resolve-symbol loc old-scope-info)])))))

(defn transpose [rings]
  (let [transpose-ring (fn [[node-id node-ids]] (map (fn [id] [id node-id]) node-ids))]
    (into {} (mapcat transpose-ring rings))))

(defn fill-in-related [analysis rings]
  (let [index (transpose rings)
        lookup-related (fn [[node-id data]]
                         (if-let [ring-id (get index node-id)]
                           [node-id (assoc data :related (set (rings ring-id)))]
                           [node-id data]))]
    (into {} (map lookup-related analysis))))

(defn analyze-scopes [analysis loc]
  (binding [*scope-id* 0
            *scope-locals* {}
            *related-rings* {}]
    (let [new-analysis (-> (analyze-scope nil loc)
                         (fill-in-related *related-rings*))]
      (helpers/deep-merge analysis new-analysis))))
