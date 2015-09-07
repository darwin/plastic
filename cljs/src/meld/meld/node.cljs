(ns meld.node
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.ids :as ids]))

(def compounds #{:list :vector :map :set :unit})

(defn strip-meta [o]
  (if (implements? IWithMeta o) (with-meta o nil) o))

(defn detect-token-type [token tag]
  (case token
    :eof-sentinel :eof
    (if (compounds tag) :compound :token)))

(defn detect-token-tag [token]
  (cond
    (symbol? token) :symbol
    (string? token) :string
    (regexp? token) :regexp
    (number? token) :number
    (seq? token) :list
    (vector? token) :vector
    (map? token) :map
    (set? token) :set))

(defn classify [token]
  (let [tag (detect-token-tag token)]
    (merge {:type (detect-token-type token tag)
            :tag  tag})))

(defn make-node-from-token [token info children]
  (let [classification (classify token)
        node (merge info
               classification
               (if-not (= :eof (:type classification))
                 {:sexpr (strip-meta token)})
               (if-not (empty? children)
                 {:children children}))]
    node))

; -------------------------------------------------------------------------------------------------------------------

(defn make-unit [top-level-nodes-ids source name]
  {:id       (ids/next-node-id!)
   :tag      :unit
   :type     :compound
   :start    0
   :end      (count source)
   :source   source
   :name     name
   :children top-level-nodes-ids})

(defn get-end [node]
  (:end node))

; -------------------------------------------------------------------------------------------------------------------

(defn insert-id* [inserter ids marker-id new-id]
  (loop [ids ids
         res []]
    (if-let [id (first ids)]
      (if (identical? id marker-id)
        (recur (rest ids) (inserter res marker-id new-id))                                                            ; TODO: I think here we can concat the rest, didn't work for me
        (recur (rest ids) (conj res id)))
      res)))

(def insert-id-right (partial insert-id* (fn [coll marker-id new-id] (-> coll (conj marker-id) (conj new-id)))))
(def insert-id-left (partial insert-id* (fn [coll marker-id new-id] (-> coll (conj new-id) (conj marker-id)))))

(defn remove-id [ids id]
  (cljs.core/remove #{id} ids))

(defn prepend-id [ids id]
  (cons id ids))

(defn append-id [ids id]
  (conj ids id))

; -------------------------------------------------------------------------------------------------------------------

(defn get-id [node]
  {:pre [node]}
  (:id node))

(defn set-id [node id]
  {:pre [node]}
  (assoc node :id id))

(defn get-type [node]
  {:pre [node]}
  (:type node))

(defn set-type [node type]
  {:pre [node]}
  (assoc node :type type))

(defn get-children [node]
  {:pre [node]}
  (:children node))

(defn set-children [node children]
  {:pre [node]}
  (assoc node :children children))

(defn get-parent [node]
  {:pre [node]}
  (:parent node))

(defn set-parent [node parent]
  {:pre [node]}
  (assoc node :parent parent))

(defn peek-right [node id]
  {:pre [node
         (number? id)]}
  (loop [children (:children node)]
    (if-let [child-id (first children)]
      (if (identical? child-id id)
        (second children)
        (recur (rest children))))))

(defn peek-left [node id]
  {:pre [node
         (number? id)]}
  (loop [prev-id nil
         children (:children node)]
    (if-let [child-id (first children)]
      (if (identical? child-id id)
        prev-id
        (recur child-id (rest children))))))

(defn rightmost-child [node]
  {:pre [node]}
  (last (get-children node)))

(defn leftmost-child [node]
  {:pre [node]}
  (first (get-children node)))

(defn remove-child [node id]
  {:pre [node
         (number? id)]}
  (set-children node (remove-id (get-children node) id)))

(defn insert-child-right [node where-id new-id]
  {:pre [node
         (number? new-id)
         (number? where-id)]}
  (set-children node (insert-id-right (get-children node) where-id new-id)))

(defn insert-child-leftmost [node new-id]
  {:pre [node
         (number? new-id)]}
  (set-children node (prepend-id (get-children node) new-id)))

(defn insert-child-rightmost [node new-id]
  {:pre [node
         (number? new-id)]}
  (set-children node (append-id (get-children node) new-id)))

(defn ^boolean whitespace? [node]
  {:pre [node]}
  (#{:whitespace} (get-type node)))
