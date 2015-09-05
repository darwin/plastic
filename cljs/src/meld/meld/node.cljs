(ns meld.node
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]))

(defn strip-meta [o]
  (if (implements? IWithMeta o) (with-meta o nil) o))

(def compounds #{:list :vector :map :set :unit})

(defn detect-token-type [token tag]
  (case token
    ::eof-sentinel :eof
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

; -------------------------------------------------------------------------------------------------------------------

(defn make-node [token info children]
  (let [classification (classify token)
        node (merge info
               classification
               (if-not (= :eof (:type classification))
                 {:sexpr (strip-meta token)})
               (if-not (empty? children)
                 {:children children}))]
    node))

; -------------------------------------------------------------------------------------------------------------------

(defn make-unit [top-level-nodes source name]
  {:tag      :unit
   :type     :compound
   :start    0
   :end      (count source)
   :source   source
   :name     name
   :children top-level-nodes})
