(ns plastic.main.validator
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]])
  (:require [schema.core :as s :include-macros true]
            [plastic.validator :refer [cached factory]]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.toolkit.id :as id]))

; -------------------------------------------------------------------------------------------------------------------

(def TODO s/Any)

(def editor-id s/Int)

(def node-id (s/either s/Int s/Str))

(def maybe-node-id (s/maybe node-id))

(def node-id-list [node-id])

(def node-id-set #{node-id})

(def line-number s/Int)

(def form-id s/Int)

(def scope-id s/Int)

(def children-table-row
  [(s/one {s/Keyword TODO} "row options")
   node-id])

(def children-table
  [(s/one {s/Keyword TODO} "table options")
   children-table-row])

(def layout-info
  (cached
    {:id                        node-id
     :tag                       s/Keyword
     (s/optional-key :line)     line-number
     (s/optional-key :text)     s/Str
     (s/optional-key :type)     s/Keyword
     (s/optional-key :children) (s/either children-table node-id-list)
     s/Keyword                  TODO}))

(def editor-layout
  (cached {form-id {node-id layout-info}}))

(def selectable-info
  (cached
    {:id                        node-id
     :tag                       s/Keyword
     (s/optional-key :line)     line-number
     (s/optional-key :text)     s/Str
     (s/optional-key :type)     s/Keyword
     (s/optional-key :children) (s/either children-table node-id-list)
     s/Keyword                  TODO}))

(def editor-selectables
  (cached {form-id {node-id selectable-info}}))

(def spatial-item
  (cached
    {:id                    node-id
     :tag                   s/Keyword
     (s/optional-key :line) line-number
     (s/optional-key :text) s/Str
     (s/optional-key :type) s/Keyword
     s/Keyword              TODO}))

(def spatial-info
  (cached {line-number [spatial-item]}))

(def editor-spatial-web
  (cached {form-id spatial-info}))

(def structural-item
  (cached
    {:left  maybe-node-id
     :right maybe-node-id
     :up    maybe-node-id
     :down  maybe-node-id}))

(def structural-info
  {node-id structural-item})

(def editor-structural-web
  (cached {form-id structural-info}))

(def analysis-scope
  {:id       scope-id
   :depth    s/Int
   s/Keyword TODO})

(def analysis-item
  (cached
    {(s/optional-key :scope)        analysis-scope
     (s/optional-key :parent-scope) TODO
     (s/optional-key :decl-scope)   analysis-scope
     (s/optional-key :related)      node-id-set
     s/Keyword                      TODO}))

(def analysis-info
  {node-id analysis-item})

(def editor-analysis
  (cached {form-id analysis-info}))

(def editor-render-state
  (cached {:order [form-id]}))

(def editor-members
  {:id                               editor-id
   :uri                              s/Str
   (s/optional-key :render-state)    editor-render-state
   (s/optional-key :layout)          editor-layout
   (s/optional-key :selectables)     editor-selectables
   (s/optional-key :spatial-web)     editor-spatial-web
   (s/optional-key :structural-web)  editor-structural-web
   (s/optional-key :analysis)        editor-analysis
   (s/optional-key :highlight)       node-id-set
   (s/optional-key :cursor)          node-id-set
   (s/optional-key :selection)       node-id-set
   (s/optional-key :puppets)         node-id-set
   (s/optional-key :focused-form-id) node-id-set
   (s/optional-key :editing)         node-id-set
   (s/optional-key :inline-editor)   TODO
   (s/optional-key :xform-report)    TODO})

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

(def editor
  (cached
    (s/both
      (s/protocol editor/IEditor)
      editor-members
      (s/pred editor-cursor-consistent?)
      (s/pred editor-selection-consistent?)
      (s/pred editor-highlight-consistent?)
      (s/pred editor-puppets-consistent?)
      (s/pred editor-focused-form-id-consistent?)
      (s/pred editor-editing-consistent?))))

(def undo-redo-item
  [(s/one s/Str "description")
   (s/one editor "editor snapshot")])

(def undo-redo-queue
  (s/both
    (s/pred vector?)
    [undo-redo-item]))

(def editor-undo-redo
  (cached {(s/optional-key :undos) undo-redo-queue
           (s/optional-key :redos) undo-redo-queue}))

(def root-schema
  {:settings  {s/Keyword s/Bool}
   :undo-redo {editor-id editor-undo-redo}
   :editors   {editor-id editor}})

; -------------------------------------------------------------------------------------------------------------------

(def benchmark? (or plastic.env.bench-db-validation plastic.env.bench-main-db-validation))

(def create (partial factory "main-db" benchmark? (s/checker root-schema) (fn [] plastic.env.*current-main-event*)))
