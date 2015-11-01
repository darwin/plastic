(ns plastic.services
  (:require [plastic.logging :refer-macros [log info warn error group group-end fancy-log]]))

; -------------------------------------------------------------------------------------------------------------------

(defn get-service [context name]
  (let [service (get-in context [:services :services name])]
    (assert service)
    service))