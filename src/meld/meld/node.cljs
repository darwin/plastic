(ns meld.node
  (:refer-clojure :exclude [string? keyword? regexp? seq? vector? map? set? symbol? list?])
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [meld.ids :as ids]
            [meld.util :refer [remove-nil-keys]]))

; -------------------------------------------------------------------------------------------------------------------

(def compound-tags #{:file :unit :list :vector :map :set :meta :quote :deref :unquote :unquote-splicing})

(defn valid-id? [id]
  (number? id))

(defn ^boolean compound? [node]
  {:pre [node]}
  (compound-tags (:tag node)))

; -------------------------------------------------------------------------------------------------------------------

(defn get-id [node]
  {:pre [node]}
  (:id node))

(defn set-id [node id]
  {:pre [node]}
  (assoc node :id id))

(defn get-type [node]
  {:pre [node]}
  (if (compound? node) :compound :token))

(defn get-children [node]
  {:pre [node
         (or (not (compound? node)) (seqable? (:children node)))]}
  (if (compound? node)
    (:children node)))

(defn set-children [node children]
  {:pre [node
         (compound? node)
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

(defn get-sexpr [node]
  {:pre [node]}
  (:sexpr node))

(defn set-sexpr [node sexpr]
  {:pre [node]}
  (assoc node :sexpr sexpr))


; -------------------------------------------------------------------------------------------------------------------

(defn strip-meta [o]
  (if (implements? IWithMeta o)
    (with-meta o nil)
    o))

(defn meta? [node]
  (= (first (get-source node)) "^"))                                                                                  ; unfortunately we have no better way how to detect this case, meta information from tools.reader is messy

(defn- quote? [sexpr]
  (= (first sexpr) 'quote))

(defn- deref? [sexpr]
  (= (first sexpr) 'clojure.core/deref))

(defn- unquote-splicing? [sexpr]
  (= (first sexpr) 'clojure.core/unquote-splicing))

(defn- unquote? [sexpr]
  (= (first sexpr) 'clojure.core/unquote))

(defn detect-node-tag [node]
  (let [sexpr (get-sexpr node)]
    (cond
      (meta? node) :meta
      (cljs.core/string? sexpr) :string
      (cljs.core/keyword? sexpr) :keyword
      (cljs.core/regexp? sexpr) :regexp
      (cljs.core/symbol? sexpr) :symbol
      (cljs.core/number? sexpr) :number
      (cljs.core/vector? sexpr) :vector
      (cljs.core/map? sexpr) :map
      (cljs.core/set? sexpr) :set
      (cljs.core/list? sexpr) (cond
                                (quote? sexpr) :quote
                                (deref? sexpr) :deref
                                (unquote? sexpr) :unquote
                                (unquote-splicing? sexpr) :unquote-splicing
                                :else :list)
      :else (assert "unable to detect node tag"))))

(defn detect-and-set-node-tag [node]
  (set-tag node (detect-node-tag node)))

(defn make-node-from-token [token info children]
  (let [node (-> info
               (set-sexpr (strip-meta token))
               (detect-and-set-node-tag)
               (remove-nil-keys))]
    (if (compound? node)
      (set-children node children)
      (do
        (assert (nil? (seq children)) (str "node is not a compound, but has children: " (pr-str node)))
        node))))

; -------------------------------------------------------------------------------------------------------------------

(defn make-file [source children name]
  (remove-nil-keys
    {:tag          :file
     :name         name
     :start        0
     :end          (count source)
     :source       source
     :revisioning? true
     :children     children}))

(defn make-unit [source children start end]
  (remove-nil-keys
    {:tag          :unit
     :start        start
     :end          end
     :source       source
     :revisioning? true
     :children     children}))

(defn make-node [tag source]
  (remove-nil-keys
    {:tag    tag
     :source source}))

(defn make-symbol [source]
  (make-node :symbol source))

(defn make-string [source]
  (make-node :string source))

(defn make-keyword [source]
  (make-node :keyword source))

(defn make-regexp [source]
  (make-node :regexp source))

(defn make-list []
  (make-node :list ""))

(defn make-vector []
  (make-node :vector ""))

(defn make-map []
  (make-node :map ""))

(defn make-set []
  (make-node :set ""))

(defn make-linebreak []
  (make-node :linebreak "\n"))

(defn make-comment [content]
  (-> (make-node :comment nil)
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

(defn replace-child [node old-id new-id]
  {:pre [node
         (valid-id? old-id)
         (valid-id? new-id)]}
  (set-children node (ids/replace-id (get-children node) old-id new-id)))

; -------------------------------------------------------------------------------------------------------------------

(defn ^boolean whitespace? [node]
  {:pre [node]}
  (keyword-identical? :whitespace (get-tag node)))

(defn ^boolean linebreak? [node]
  {:pre [node]}
  (keyword-identical? :linebreak (get-tag node)))

(defn ^boolean comment? [node]
  {:pre [node]}
  (keyword-identical? :comment (get-tag node)))

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

; -------------------------------------------------------------------------------------------------------------------

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

; -------------------------------------------------------------------------------------------------------------------

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

; -------------------------------------------------------------------------------------------------------------------

(defn get-desc [node]
  (case (get-type node)
    :compound (str (get-tag node))
    (case (get-tag node)
      :linebreak "↓"
      :comment (get-content node)
      (get-source node))))