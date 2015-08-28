(ns plastic.main.validator
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]])
  (:require [schema.core :as s :include-macros true]
            [plastic.validator :refer [cached factory TODO! string! editor-id! node-id-set! editor-render-state!
                                       editor-layout! editor-selectables! editor-spatial-web! editor-structural-web!
                                       editor-analysis! integer! key? bool! keyword!]]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.toolkit.id :as id]))

; -------------------------------------------------------------------------------------------------------------------

(def editor-members!
  {:id                     editor-id!
   :uri                    string!
   (key? :render-state)    editor-render-state!
   (key? :layout)          editor-layout!
   (key? :selectables)     editor-selectables!
   (key? :spatial-web)     editor-spatial-web!
   (key? :structural-web)  editor-structural-web!
   (key? :analysis)        editor-analysis!
   (key? :highlight)       node-id-set!
   (key? :cursor)          node-id-set!
   (key? :selection)       node-id-set!
   (key? :puppets)         node-id-set!
   (key? :focused-form-id) node-id-set!
   (key? :editing)         node-id-set!
   (key? :inline-editor)   TODO!
   (key? :xform-report)    TODO!})

(defn editor-nodes-present-in-layout? [editor node-or-nodes]
  (let [things (if (coll? node-or-nodes)
                 node-or-nodes
                 (if-not (nil? node-or-nodes)
                   [node-or-nodes]))]
    (every? #(editor/get-form-id-for-node-id editor (id/id-part %)) things)))

(defn editor-cursor-consistent? [editor]
  (editor-nodes-present-in-layout? editor (editor/get-cursor editor)))

(defn editor-selection-consistent? [editor]
  (editor-nodes-present-in-layout? editor (editor/get-selection editor)))

(defn editor-highlight-consistent? [editor]
  (editor-nodes-present-in-layout? editor (editor/get-highlight editor)))

(defn editor-puppets-consistent? [editor]
  (editor-nodes-present-in-layout? editor (editor/get-puppets editor)))

(defn editor-focused-form-id-consistent? [editor]
  (editor-nodes-present-in-layout? editor (editor/get-focused-form-id editor)))

(defn editor-editing-consistent? [editor]
  (editor-nodes-present-in-layout? editor (editor/get-editing editor)))

(def editor!
  (cached
    (s/both
      (s/protocol editor/IEditor)
      editor-members!
      (s/pred editor-cursor-consistent?)
      (s/pred editor-selection-consistent?)
      (s/pred editor-highlight-consistent?)
      (s/pred editor-puppets-consistent?)
      (s/pred editor-focused-form-id-consistent?)
      (s/pred editor-editing-consistent?))))

(def undo-redo-item!
  [(s/one string! "description")
   (s/one editor! "editor snapshot")])

(def undo-redo-queue!
  (s/both
    (s/pred vector?)
    [undo-redo-item!]))

(def editor-undo-redo!
  (cached {(key? :undos) undo-redo-queue!
           (key? :redos) undo-redo-queue!}))

(def root-schema!
  {:settings  {keyword! bool!}
   :undo-redo {editor-id! editor-undo-redo!}
   :editors   {editor-id! editor!}})

(def checker (s/checker root-schema!))

; -------------------------------------------------------------------------------------------------------------------

(def benchmark? (or plastic.env.bench-db-validation plastic.env.bench-main-db-validation))

(def create (partial factory "main-db" benchmark? checker (fn [] plastic.env.*current-main-event*)))
