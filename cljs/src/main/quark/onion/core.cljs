(ns quark.onion.core
  (:require [quark.onion.inface :as inface]
            [quark.onion.api :refer [$ TextEditor atom]]
            [quark.onion.remounter]
            [quark.onion.api :refer [File]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn load-file-content [uri cb]
  {:pre [File]}
  (let [file (File. uri)
        content (.read file)]
    (.then content cb)))

(defn prepare-editor-instance [editor-id]
  (let [atom-editor-view (get @inface/ids->views editor-id)
        _ (assert atom-editor-view)
        mini-editor (.-miniEditor atom-editor-view)
        _ (assert mini-editor)
        mini-editor-view (.-miniEditorView atom-editor-view)
        _ (assert mini-editor-view)]
    {:editor mini-editor
     :view   mini-editor-view}))