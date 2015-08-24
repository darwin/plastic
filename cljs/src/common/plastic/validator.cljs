(ns plastic.validator
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]])
  (:require [schema.core :as s :include-macros true]
            [goog.object :as gobj]))

; shared validation utils

; see https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/WeakSet
(assert js/WeakSet)
(defonce *valid-objects* (js/WeakSet.))

(defn check-or-update-cache! [schema obj]
  (if (.has *valid-objects* obj)
    true
    (when (nil? (s/check schema obj))
      (.add *valid-objects* obj)
      true)))

;  to speed up validation we can cache some expensive validation results
(defn cached [schema]
  (s/if (partial check-or-update-cache! schema) s/Any schema))
