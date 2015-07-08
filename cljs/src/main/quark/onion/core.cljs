(ns quark.onion.core
  (:require [quark.onion.inface]
            [quark.onion.remounter]
            [quark.onion.api :refer [File]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn load-file-content [uri cb]
  {:pre [File]}
  (let [file (File. uri)
        content (.read file)]
    (.then content cb)))

(defn overlay-mini-editor [editor-id dom-node text]
  (log "overlay-mini-editor" editor-id dom-node text))