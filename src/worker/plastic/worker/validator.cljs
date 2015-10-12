(ns plastic.worker.validator
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]])
  (:require [schema.core :as s :include-macros true]
            [plastic.validator :refer [cached factory editor-id! editor-render-state! editor-layout! TODO!
                                       editor-selectables! editor-spatial-web! editor-structural-web! editor-analysis!
                                       unit-id! anything string! key? all! editor-units!]]
            [plastic.worker.editor.model :as editor]))

; -------------------------------------------------------------------------------------------------------------------

(def editor-previously-layouted-units!
  (cached {unit-id! TODO!}))

(def editor-members!
  {:id                               editor-id!
   :uri                              string!
   (key? :source)                    string!
   (key? :parse-tree)                anything
   (key? :meld)                      anything
   (key? :units)                     editor-units!
   (key? :previously-layouted-units) editor-previously-layouted-units!
   (key? :layout)                    editor-layout!
   (key? :selectables)               editor-selectables!
   (key? :spatial-web)               editor-spatial-web!
   (key? :structural-web)            editor-structural-web!
   (key? :analysis)                  editor-analysis!
   (key? :xform-report)              TODO!})

(def editor!
  (cached (all!
            (s/protocol editor/IEditor)
            editor-members!)))

(def undo-redo-item!
  [(s/one string! "description")
   (s/one editor! "editor snapshot")])

(def undo-redo-queue!
  (all!
    (s/pred vector?)
    [undo-redo-item!]))

(def editor-undo-redo!
  (cached {(key? :undos) undo-redo-queue!
           (key? :redos) undo-redo-queue!}))
(def root-schema!
  {:undo-redo {editor-id! editor-undo-redo!}
   :editors   {editor-id! editor!}})

(def checker (s/checker root-schema!))

; -------------------------------------------------------------------------------------------------------------------

(def benchmark? (or plastic.env.bench-db-validation plastic.env.bench-worker-db-validation))

(def create (partial factory "worker-db" benchmark? checker (fn [] plastic.env.*current-worker-event*)))