(ns quark.cogs.editor.analysis.scopes
  (:require [quark.util.helpers :as helpers]
            [rewrite-clj.node :as node])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defonce ^:dynamic scope-id 0)

(defn next-scope-id! []
  (set! scope-id (inc scope-id))
  scope-id)

(def scope-openers
  {'defn  :params
   'defn- :params
   'fn    :params
   'let   :pairs})

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

(defn node-scope [node]
  (if (= (node/tag node) :list)
    (if-let [opener-type (scope-openers (node/sexpr (first (node/children node))))]
      {:id     (next-scope-id!)
       :locals (collect-params node opener-type)})))

(defn scope-related-nodes [nodes]
  (remove #(or (node/whitespace? %) (node/comment? %)) nodes))

(defn analyze-scope [scope-info node]
  (let [child-scopes (fn [scope node]
                       (if (node/inner? node)
                         (let [res (map (partial analyze-scope scope) (scope-related-nodes (node/children node)))]
                           (apply merge res))
                         {}))]
    (if-let [new-scope (node-scope node)]
      (let [new-scope-info {:scope new-scope :parent-scope scope-info}]
        (merge
          {(:id node) new-scope-info}
          (child-scopes new-scope-info node)))
      (merge
        {(:id node) scope-info}
        (child-scopes scope-info node)))))

(defn analyze-scopes [node info]
  (binding [scope-id 0]
    (helpers/deep-merge info (analyze-scope {:scope nil :parent-scope nil} node))))