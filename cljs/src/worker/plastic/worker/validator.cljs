(ns plastic.worker.validator
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]])
  (:require [schema.core :as s :include-macros true]
            [plastic.validator :refer [cached factory]]
            [plastic.worker.editor.model :as editor]))

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

(def editor-render-state
  (cached {:order [form-id]}))

(def editor-previously-layouted-forms
  (cached {form-id TODO}))

(def editor-members
  {:id                                         editor-id
   :uri                                        s/Str
   (s/optional-key :text)                      s/Str
   (s/optional-key :parse-tree)                s/Any
   (s/optional-key :render-state)              editor-render-state
   (s/optional-key :previously-layouted-forms) editor-previously-layouted-forms
   (s/optional-key :xform-report)              TODO})

(def editor
  (cached (s/both
            (s/protocol editor/IEditor)
            editor-members)))

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
  {:undo-redo {editor-id editor-undo-redo}
   :editors   {editor-id editor}})

; -------------------------------------------------------------------------------------------------------------------

(def benchmark? (or plastic.env.bench-db-validation plastic.env.bench-worker-db-validation))

(def create (partial factory "worker-db" benchmark? (s/checker root-schema) (fn [] plastic.env.*current-worker-event*)))
