(ns plastic.worker.editor.xforms
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.frame :refer-macros [main-dispatch]]
            [plastic.worker.editor.xforms.editing :as editing]
            [plastic.worker.editor.model :as editor]
            [plastic.worker.frame.undo :as undo]))

; -------------------------------------------------------------------------------------------------------------------

(def xforms
  {:edit                              editing/edit
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

; -------------------------------------------------------------------------------------------------------------------

(defn dispatch-xform-on-editor [editor xform & args]
  (if-let [handler (xform xforms)]
    (apply handler editor args)
    (error (str "Unknown editor xform for dispatch '" xform "'"))))

(defn dispatch-xform [context db [editor-id xform & args]]
  (undo/open-undo-session! editor-id (name xform))
  (or
    (let [editor (editor/set-context (get-in db [:editors editor-id]) context)]
      (if-let [new-editor (apply dispatch-xform-on-editor editor xform args)]
        (if-not (identical? new-editor editor)
          (let [xform-report (editor/get-xform-report new-editor)]
            (undo/set-undo-report! editor-id xform-report)
            (main-dispatch context [:editor-announce-xform editor-id xform-report])
            (update db :editors assoc editor-id (editor/remove-xform-report (editor/strip-context new-editor)))))))
    db))