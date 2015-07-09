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

(defn overlay-mini-editor [editor-id form-id selectable-id geometry text]
  (let [atom-editor-view (get @inface/ids->views editor-id)
        _ (assert atom-editor-view)
        mini-editor (.-miniEditor atom-editor-view)
        _ (assert mini-editor)
        mini-editor-view (.-miniEditorView atom-editor-view)
        _ (assert mini-editor-view)
        $mini-editor-view ($ mini-editor-view)
        $selectable (.find atom-editor-view (str "[data-qid='" selectable-id "']"))
        _ (assert (aget $selectable 0))
        selectable-offset (.offset $selectable)
        _ (assert selectable-offset)]
    (log "overlay-mini-editor" editor-id form-id geometry)
    (.offset $mini-editor-view selectable-offset)
    (.width $mini-editor-view (:width geometry))
    (.height $mini-editor-view (:height geometry))
    (.setText mini-editor text)))

(defn prepare-editor-instance [editor-id]
  (let [atom-editor-view (get @inface/ids->views editor-id)
        _ (assert atom-editor-view)
        mini-editor (.-miniEditor atom-editor-view)
        _ (assert mini-editor)
        mini-editor-view (.-miniEditorView atom-editor-view)
        _ (assert mini-editor-view)]
    {:editor mini-editor
     :view   mini-editor-view}))