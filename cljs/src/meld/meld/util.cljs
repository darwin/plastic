(ns meld.util
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]))

; -------------------------------------------------------------------------------------------------------------------

(defn update! [m k f & args]
  (let [o (get m k)]
    (assoc! m k (apply f o args))))
