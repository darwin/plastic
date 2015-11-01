(ns plastic.onion.host
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.services :refer [get-service]]))

; -------------------------------------------------------------------------------------------------------------------

(defn load-file-content [context uri cb]
  (let [File (get-service context :File)
        file (File. uri)
        content (.read file)]
    (.then content cb)))