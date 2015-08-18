(ns plastic.worker.editor.xforms
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [react! dispatch]])
  (:require [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.schema.paths :as paths]
            [plastic.worker.editor.xforms.editing :as editing]))

(def ops
  {:edit-node                         editing/edit-node
   :enter                             editing/enter
   :alt-enter                         editing/alt-enter
   :space                             editing/space
   :backspace                         editing/backspace
   :delete                            editing/delete
   :alt-delete                        editing/alt-delete
   :open-list                         editing/open-list
   :open-vector                       editing/open-vector
   :open-map                          editing/open-map
   :open-set                          editing/open-set
   :open-fn                           editing/open-fn
   :open-meta                         editing/open-meta
   :open-quote                        editing/open-quote
   :open-deref                        editing/open-deref
   :insert-placeholder-as-first-child editing/insert-placeholder-as-first-child})

; ----------------------------------------------------------------------------------------------------------------------

(defn dispatch-xform [editors [editor-id xform & args]]
  (let [old-editor (get editors editor-id)]
    (if-let [handler (xform ops)]
      (if-let [new-editor (apply handler old-editor args)]
        (assoc-in editors [editor-id] new-editor)
        editors)
      (do
        (error (str "Unknown editor xform for dispatch '" xform "'"))
        editors))))

; ----------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-xform paths/editors-path dispatch-xform)