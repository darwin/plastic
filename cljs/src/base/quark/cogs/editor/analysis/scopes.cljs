(ns quark.cogs.editor.analysis.scopes
  (:require [cljs.core.async :refer [<! timeout]]
            [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [quark.util.helpers :as helpers]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [clojure.string :as string]
            [rewrite-clj.node.stringz :refer [StringNode]]
            [rewrite-clj.node.keyword :refer [KeywordNode]]
            [quark.cogs.editor.analyzer :refer [analyze-full]]
            [quark.cogs.editor.utils :refer [essential-children node-walker node-interesting? leaf-nodes ancestor-count make-path make-zipper collect-all-right]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]]))

(defonce ^:dynamic scope-id 0)

(defn next-scope-id! []
  (set! scope-id (inc scope-id))
  scope-id)

(declare analyze-scope)

(def scope-openers
  {'defn :params
   'defn- :params
   'fn   :params
   'let  :pairs})

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

(defn child-scopes [scope node]
  (if (node/inner? node)
    (let [res (map (partial analyze-scope scope) (essential-children node))]
      (apply merge res))
    {}))

(defn analyze-scope [scope-info node]
  (if-let [new-scope (node-scope node)]
    (let [new-scope-info {:scope new-scope :parent-scope scope-info}]
      (merge
        {node new-scope-info}
        (child-scopes new-scope-info node)))
    (merge
      {node scope-info}
      (child-scopes scope-info node))))

(defn analyze-scopes [node info]
  (binding [scope-id 0]
    (helpers/deep-merge info (analyze-scope {:scope nil :parent-scope nil} node))))