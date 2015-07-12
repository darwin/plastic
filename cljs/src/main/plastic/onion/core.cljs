(ns plastic.onion.core
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.onion.inface :as inface]
            [plastic.onion.api :refer [$]]
            [plastic.onion.remounter]
            [plastic.onion.api :refer [File]]
            [plastic.cogs.editor.render.dom :as dom]))

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

(defn append-inline-editor [editor-id dom-node]
  (let [inline-editor-view (get-atom-inline-editor-view-instance editor-id)]
    (.append ($ dom-node) inline-editor-view)))

(defn is-inline-editor-focused? [editor-id]
  (let [inline-editor-view (get-atom-inline-editor-view-instance editor-id)]
    (dom/has-class? inline-editor-view "is-focused")))

(defn focus-inline-editor [editor-id]
  (let [inline-editor-view (get-atom-inline-editor-view-instance editor-id)]
    (.focus inline-editor-view)))

(def known-editor-types #{:symbol :keyword :doc :string})
(def known-editor-types-classes (apply str (interpose " " (map name known-editor-types))))

(defn set-editor-type-as-class-name [inline-editor-view editor-type]
  (-> ($ inline-editor-view)
    (.removeClass known-editor-types-classes)
    (.addClass (name editor-type))))

(defn preprocess-text-before-editing [editor-type text]
  text)

(defn postprocess-text-after-editing [editor-type text]
  text)

(defn setup-inline-editor-for-editing [editor-id editor-type text]
  (let [inline-editor (get-atom-inline-editor-instance editor-id)
        inline-editor-view (get-atom-inline-editor-view-instance editor-id)
        initial-text (preprocess-text-before-editing editor-type text)]
    (log "editor type" editor-type text initial-text)
    ; synchronous updates prevent intermittent jumps
    ; it has correct dimentsions before it gets appended in the DOM
    (.setUpdatedSynchronously inline-editor-view true)
    (set-editor-type-as-class-name inline-editor-view editor-type)
    (.setText inline-editor initial-text)
    (.selectAll inline-editor)
    (.setUpdatedSynchronously inline-editor-view false)))

(defn is-inline-editor-modified? [editor-id]
  (let [inline-editor (get-atom-inline-editor-instance editor-id)]
    (.isModified inline-editor)))

(defn get-inline-editor-type [editor-id]
  {:post [(contains? known-editor-types %)]}
  (let [$inline-editor-view ($ (get-atom-inline-editor-view-instance editor-id))]
    (keyword (some #(if (.hasClass $inline-editor-view (name %)) %) known-editor-types))))

(defn get-postprocessed-text-after-editing [editor-id]
  (let [inline-editor (get-atom-inline-editor-instance editor-id)
        editor-type (get-inline-editor-type editor-id)
        raw-text (.getText inline-editor)]
    (postprocess-text-after-editing editor-type raw-text)))