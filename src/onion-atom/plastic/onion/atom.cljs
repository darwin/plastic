(ns plastic.onion.atom
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.onion :refer [update-inline-editor-synchronously]])
  (:require [plastic.onion.api :refer [File $ atom-api]]))

; -------------------------------------------------------------------------------------------------------------------

(defn load-file-content [uri cb]
  {:pre [File]}
  (let [file (File. uri)
        content (.read file)]
    (.then content cb)))

