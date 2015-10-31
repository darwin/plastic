(ns plastic.onion.host
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.services :refer [get-service]]))

; -------------------------------------------------------------------------------------------------------------------

(defn load-file-content [context uri cb]
  (let [File (get-service context :File)
        file (File. uri)
        content (.read file)]
    (.then content cb)))