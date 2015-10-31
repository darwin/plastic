(ns plastic.onion.host
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.onion.api :refer [File]]))

; -------------------------------------------------------------------------------------------------------------------

(defn load-file-content [uri cb]
  {:pre [File]}
  (let [file (File. uri)
        content (.read file)]
    (.then content cb)))