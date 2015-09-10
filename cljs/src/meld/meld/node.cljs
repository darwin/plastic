(ns meld.node
  (:refer-clojure :exclude [string? keyword? regexp? seq? vector? map? set? symbol? list?])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.ids :as ids]
            [meld.util :refer [remove-nil-keys]]))

(def compound-tags #{:list :vector :map :set :unit :file})

(defn valid-id? [id]
  (number? id))

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
  {:pre  [node
          (= :compound (:type node))]
   :post [(seqable? %)]}
  (:children node))

(defn set-children [node children]
  {:pre [node
         (= :compound (:type node))
         (seqable? children)]}
  (assoc node :children children))

(defn get-parent [node]
  {:pre [node]}
  (:parent node))

(defn set-parent [node parent]
  {:pre [node]}
  (if (nil? parent)
    (dissoc node :parent)
    (assoc node :parent parent)))

(defn get-source [node]
  {:pre [node]}
  (:source node))

(defn set-source [node source]
  {:pre [node]}
  (if (nil? source)
    (dissoc node :source)
    (assoc node :source source)))

(defn get-content [node]
  {:pre [node]}
  (:content node))

(defn set-content [node content]
  {:pre [node]}
  (if (nil? content)
    (dissoc node :content)
    (assoc node :content content)))

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

; -------------------------------------------------------------------------------------------------------------------

(defn strip-meta [o]
  (if (implements? IWithMeta o) (with-meta o nil) o))

(defn detect-token-type [tag]
  (if (compound-tags tag) :compound :token))

(defn detect-token-tag [token]
  (cond
    (cljs.core/string? token) :string
    (cljs.core/keyword? token) :keyword
    (cljs.core/regexp? token) :regexp
    (cljs.core/seq? token) :list
    (cljs.core/vector? token) :vector
    (cljs.core/map? token) :map
    (cljs.core/set? token) :set
    :else :symbol))

(defn classify [token]
  (let [tag (detect-token-tag token)
        type (detect-token-type tag)]
    [type tag]))

(defn make-node-from-token [token info children]
  (let [[type tag] (classify token)]
    (cond-> info
      true (merge {:type  type
                   :tag   (or tag type)
                   :sexpr (strip-meta token)})
      true (remove-nil-keys)
      (= type :compound) (set-children children))))

; -------------------------------------------------------------------------------------------------------------------

(defn make-file [source children name]
  (remove-nil-keys
    {:id           (ids/next-node-id!)
     :type         :compound
     :tag          :file
     :name         name
     :start        0
     :end          (count source)
     :source       source
     :revisioning? true
     :children     children}))


(defn make-unit [source children start end]
  (remove-nil-keys
    {:id           (ids/next-node-id!)
     :type         :compound
     :tag          :unit
     :start        start
     :end          end
     :source       source
     :revisioning? true
     :children     children}))

(defn make-node [type tag source]
  (remove-nil-keys
    {:id     (ids/next-node-id!)
     :type   type
     :tag    tag
     :source source}))

(defn make-token [tag source]
  (make-node :token tag source))

(defn make-compound [tag source]
  (make-node :compound tag source))

(defn make-symbol [source]
  (make-token :symbol source))

(defn make-string [source]
  (make-token :string source))

(defn make-keyword [source]
  (make-token :keyword source))

(defn make-regexp [source]
  (make-token :regexp source))

(defn make-list []
  (make-compound :list ""))

(defn make-vector []
  (make-compound :vector ""))

(defn make-map []
  (make-compound :map ""))

(defn make-set []
  (make-compound :set ""))

(defn make-linebreak []
  (make-node :linebreak :linebreak "\n"))

(defn make-comment [content]
  (-> (make-node :comment :comment nil)
    (set-content content)))

; -------------------------------------------------------------------------------------------------------------------

(defn peek-right [node id]
  {:pre [node
         (valid-id? id)]}
  (loop [children (:children node)]
    (if-let [child-id (first children)]
      (if (identical? child-id id)
        (second children)
        (recur (rest children))))))

(defn peek-left [node id]
  {:pre [node
         (valid-id? id)]}
  (loop [prev-id nil
         children (:children node)]
    (if-let [child-id (first children)]
      (if (identical? child-id id)
        prev-id
        (recur child-id (rest children))))))

(defn lefts [node id]
  {:pre [node
         (valid-id? id)]}
  (loop [collected-ids& (transient [])
         children (:children node)]
    (if-let [child-id (first children)]
      (if (identical? child-id id)
        (persistent! collected-ids&)
        (recur (conj! collected-ids& child-id) (rest children))))))

(defn rights [node id]
  {:pre [node
         (valid-id? id)]}
  (loop [children (:children node)]
    (if-let [child-id (first children)]
      (if (identical? child-id id)
        (vec (rest children))
        (recur (rest children))))))

(defn rightmost-child [node]
  {:pre [node]}
  (last (get-children node)))

(defn leftmost-child [node]
  {:pre [node]}
  (first (get-children node)))

(defn remove-child [node id]
  {:pre [node
         (valid-id? id)]}
  (set-children node (ids/remove-id (get-children node) id)))

(defn insert-child-right [node where-id new-id]
  {:pre [node
         (valid-id? new-id)
         (valid-id? where-id)]}
  (set-children node (ids/insert-id-right (get-children node) where-id new-id)))

(defn insert-child-left [node where-id new-id]
  {:pre [node
         (valid-id? new-id)
         (valid-id? where-id)]}
  (set-children node (ids/insert-id-left (get-children node) where-id new-id)))

(defn insert-child-leftmost [node new-id]
  {:pre [node
         (valid-id? new-id)]}
  (set-children node (ids/prepend-id (get-children node) new-id)))

(defn insert-child-rightmost [node new-id]
  {:pre [node
         (valid-id? new-id)]}
  (set-children node (ids/append-id (get-children node) new-id)))

; -------------------------------------------------------------------------------------------------------------------

(defn ^boolean whitespace? [node]
  {:pre [node]}
  (keyword-identical? :whitespace (get-type node)))

(defn ^boolean linebreak? [node]
  {:pre [node]}
  (keyword-identical? :linebreak (get-type node)))

(defn ^boolean comment? [node]
  {:pre [node]}
  (keyword-identical? :comment (get-type node)))

(defn ^boolean compound? [node]
  {:pre [node]}
  (keyword-identical? :compound (get-type node)))

(defn ^boolean string? [node]
  {:pre [node]}
  (keyword-identical? :string (get-tag node)))

(defn ^boolean keyword? [node]
  {:pre [node]}
  (keyword-identical? :keyword (get-tag node)))

(defn ^boolean regexp? [node]
  {:pre [node]}
  (keyword-identical? :regexp (get-tag node)))

(defn ^boolean symbol? [node]
  {:pre [node]}
  (keyword-identical? :symbol (get-tag node)))

(defn ^boolean list? [node]
  {:pre [node]}
  (keyword-identical? :list (get-tag node)))

(defn ^boolean vector? [node]
  {:pre [node]}
  (keyword-identical? :vector (get-tag node)))

(defn ^boolean map? [node]
  {:pre [node]}
  (keyword-identical? :map (get-tag node)))

(defn ^boolean set? [node]
  {:pre [node]}
  (keyword-identical? :set (get-tag node)))

(defn ^boolean unit? [node]
  {:pre [node]}
  (keyword-identical? :unit (get-tag node)))

(defn ^boolean revisioning? [node]
  {:pre [node]}
  (:revisioning? node))

(defn get-revision [node]
  {:pre [node]}
  (or (:revision node) 0))

(defn update-revision [node f & args]
  {:pre [node
         (revisioning? node)]}
  (apply update node :revision f args))

(defn inc-revision [node]
  (update-revision node inc))

(defn get-sexpr [node]
  {:pre [node]}
  (:sexpr node))

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