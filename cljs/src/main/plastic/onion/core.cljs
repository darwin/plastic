(ns plastic.onion.core
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.onion.inface :as inface]
            [plastic.onion.api :refer [$ TextEditor atom]]
            [plastic.onion.remounter]
            [plastic.onion.api :refer [File]]))

(defn load-file-content [uri cb]
  {:pre [File]}
  (let [file (File. uri)
        content (.read file)]
    (.then content cb)))

(defn get-atom-inline-editor-instance [editor-id]
  (let [atom-editor-view (get @inface/ids->views editor-id)
        _ (assert atom-editor-view)
        mini-editor (.-miniEditor atom-editor-view)]
    (assert mini-editor)
    mini-editor))

(defn get-atom-inline-editor-view-instance [editor-id]
  (let [atom-editor-view (get @inface/ids->views editor-id)
        _ (assert atom-editor-view)
        mini-editor-view (.-miniEditorView atom-editor-view)]
    (assert mini-editor-view)
    mini-editor-view))