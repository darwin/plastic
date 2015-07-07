(ns quark.onion.core
  (:require [quark.onion.inface]
            [quark.onion.remounter]
            [quark.onion.api :refer [File]]))

(defn load-file-content [uri cb]
  {:pre [File]}
  (let [file (File. uri)
        content (.read file)]
    (.then content cb)))