(ns plastic.services
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]))

; -------------------------------------------------------------------------------------------------------------------

(defn get-service [context name]
  (let [service (get-in context [:services :services name])]
    (assert service)
    service))