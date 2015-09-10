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
    (keyword? token) :keyword
    (seq? token) :list
    (vector? token) :vector
    (map? token) :map
    (set? token) :set
    :else :other))

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
  (cond-> {:id       (ids/next-node-id!)
           :tag      :unit
           :type     :compound
           :start    0
           :end      (count source)
           :source   source
           :children top-level-nodes-ids}
    name (assoc :name name)))

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

(defn get-source [node]
  {:pre [node]}
  (:source node))

(defn set-source [node source]
  {:pre [node]}
  (assoc node :source source))

(defn get-end [node]
  {:pre [node]}
  (:end node))

(defn set-end [node end]
  {:pre [node]}
  (assoc node :end end))

(defn get-start [node]
  {:pre [node]}
  (:start node))

(defn set-start [node start]
  {:pre [node]}
  (assoc node :start start))

(defn get-tag [node]
  {:pre [node]}
  (:tag node))

(defn set-tag [node tag]
  {:pre [node]}
  (assoc node :tag tag))

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
  (set-children node (ids/remove-id (get-children node) id)))

(defn insert-child-right [node where-id new-id]
  {:pre [node
         (number? new-id)
         (number? where-id)]}
  (set-children node (ids/insert-id-right (get-children node) where-id new-id)))

(defn insert-child-leftmost [node new-id]
  {:pre [node
         (number? new-id)]}
  (set-children node (ids/prepend-id (get-children node) new-id)))

(defn insert-child-rightmost [node new-id]
  {:pre [node
         (number? new-id)]}
  (set-children node (ids/append-id (get-children node) new-id)))

(defn ^boolean whitespace? [node]
  {:pre [node]}
  (#{:whitespace} (get-type node)))

(defn ^boolean compound? [node]
  {:pre [node]}
  (#{:compound} (get-type node)))

(defn get-leadspace-subnode [node]
  {:pre [node]}
  (:leadspace node))

(defn set-leadspace-subnode [node subnode]
  {:pre [node]}
  (assoc node :leadspace subnode))

(defn get-leadspace [node]
  {:pre [node]}
  (:source (get-leadspace-subnode node)))

(defn get-leadspace-size [node]
  {:pre [node]}
  (count (get-leadspace node)))

(defn get-trailspace-subnode [node]
  {:pre [node]}
  (:trailspace node))

(defn set-trailspace-subnode [node subnode]
  {:pre [node]}
  (assoc node :trailspace subnode))

(defn get-trailspace [node]
  {:pre [node]}
  (:source (get-trailspace-subnode node)))

(defn get-trailspace-size [node]
  {:pre [node]}
  (count (get-trailspace node)))

(defn get-range-start [node]
  {:pre [node]}
  (- (get-start node) (get-leadspace-size node)))

(defn get-range-end [node]
  {:pre [node]}
  (- (get-end node) (get-trailspace-size node)))

(defn get-range [node]
  {:pre [node]}
  [(get-range-start node) (get-range-end node)])

