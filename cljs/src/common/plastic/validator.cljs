(ns plastic.validator
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]])
  (:require [schema.core :as s :include-macros true]
            [goog.object :as gobj]))

; -------------------------------------------------------------------------------------------------------------------
; validation utils

(assert js/WeakSet)                                                                                                   ; see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakSet
(assert js/WeakMap)                                                                                                   ; see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakMap
(defonce *cached-validations* (js/WeakMap.))                                                                          ; object -> WeakSet of schemas

(defn cache-hit? [obj schema]
  (if-let [schema-set (.get *cached-validations* obj)]
    (.has schema-set schema)))

(defn make-weak-set [& items]
  (let [weak-set (js/WeakSet.)]
    (doseq [item items]
      (.add weak-set item))
    weak-set))

(defn cache-validation [obj schema]
  (if-let [schema-set (.get *cached-validations* obj)]
    (.add schema-set schema)
    (.set *cached-validations* obj (make-weak-set schema))))

(defn check-or-update-cache! [schema obj]
  (if (goog/isObject obj)                                                                                             ; WeakMap must have object keys, anyways primitive values are cheap to check
    (if (cache-hit? obj schema)
      true
      (when (nil? (s/check schema obj))                                                                               ; when validation fails (rare), bail out and let else branch of s/if do proper validation reporting
        (cache-validation obj schema)
        true))))

; to speed up validation we can cache some expensive validation results
; see https://github.com/Prismatic/schema/issues/256
(defn cached [schema]
  (s/if (partial check-or-update-cache! schema) s/Any schema))

(defn factory [db-name benchmark? check-fn event-name-fn]
  (fn [db]
    (if-let [e (measure-time benchmark? "VALIDATE" [db-name] (check-fn db))]
      (error (str db-name ": failed validation") e "after reset!" (event-name-fn) "invalid db:" db)
      true)))

; -------------------------------------------------------------------------------------------------------------------
; shared validation helpers

(def key? s/optional-key)

(def anything s/Any)

(def string! s/Str)

(def integer! s/Int)

(def bool! s/Bool)

(def keyword! s/Keyword)

(def TODO! anything)

(def editor-id! integer!)

(def node-id! (s/either integer! string!))

(def node-id? (s/maybe node-id!))

(def node-id-list! [node-id!])

(def node-id-set! #{node-id!})

(def line-number! integer!)

(def form-id! integer!)

(def scope-id! integer!)

(def children-table-row!
  [(s/one {keyword! TODO!} "row options")
   node-id!])

(def children-table!
  [(s/one {keyword! TODO!} "table options")
   children-table-row!])

(def layout-info!
  (cached
    {:id              node-id!
     :tag             keyword!
     (key? :line)     line-number!
     (key? :text)     string!
     (key? :type)     keyword!
     (key? :children) (s/either children-table! node-id-list!)
     keyword!         TODO!}))

(def editor-layout!
  (cached {form-id! {node-id! layout-info!}}))

(def selectable-info!
  (cached
    {:id              node-id!
     :tag             keyword!
     (key? :line)     line-number!
     (key? :text)     string!
     (key? :type)     keyword!
     (key? :children) (s/either children-table! node-id-list!)
     keyword!         TODO!}))

(def editor-selectables!
  (cached {form-id! {node-id! selectable-info!}}))

(def spatial-item!
  (cached
    {:id          node-id!
     :tag         keyword!
     (key? :line) line-number!
     (key? :text) string!
     (key? :type) keyword!
     s/Keyword    TODO!}))

(def spatial-info!
  (cached {line-number! [spatial-item!]}))

(def editor-spatial-web!
  (cached {form-id! spatial-info!}))

(def structural-item!
  (cached
    {:left  node-id?
     :right node-id?
     :up    node-id?
     :down  node-id?}))

(def structural-info!
  {node-id! structural-item!})

(def editor-structural-web!
  (cached {form-id! structural-info!}))

(def analysis-scope!
  {:id      scope-id!
   :depth   integer!
   keyword! TODO!})

(def analysis-item!
  (cached
    {(key? :scope)        analysis-scope!
     (key? :parent-scope) TODO!
     (key? :decl-scope)   analysis-scope!
     (key? :related)      node-id-set!
     keyword!             TODO!}))

(def analysis-info!
  {node-id! analysis-item!})

(def editor-analysis!
  (cached {form-id! analysis-info!}))

(def editor-render-state!
  (cached {:order [form-id!]}))
