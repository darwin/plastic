(ns plastic.validator
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]])
  (:require [schema.core :as s :include-macros true]
            [goog.object :as gobj]))

; shared validation utils

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
