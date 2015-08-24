(ns plastic.validator
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]])
  (:require [schema.core :as s :include-macros true]
            [goog.object :as gobj]))

; shared validation utils

; note: we don't clear this cache, it is just a bool per object instance (under dev environment)
(defonce *valid-ids* #js {})

(defn check-or-update-cache! [schema obj]
  (let [id (goog/getUid obj)
        lookup (gobj/get *valid-ids* id)]
    (if lookup
      true
      (when (nil? (s/check schema obj))
        (gobj/set *valid-ids* id true)
        true))))

;  to speed up validation we can cache some expensive validation results
(defn cached [schema]
  (s/if (partial check-or-update-cache! schema) s/Any schema))
