(ns plastic.worker.frame.handlers
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.frame :refer [register-handler]]
            [plastic.worker.init :refer [init]]
            [plastic.worker.editor.lifecycle :refer [add-editor! remove-editor! wire-editor!]]
            [plastic.worker.editor.analysis :refer [run-analysis]]
            [plastic.worker.editor.layout :refer [update-layout]]
            [plastic.worker.editor.loader :refer [set-source]]
            [plastic.worker.editor.parser :refer [parse-source]]
            [plastic.worker.editor.xforms :refer [dispatch-xform]]
            [plastic.undo :refer [push-undo push-redo]]
            [plastic.worker.undo :refer [undo redo]]))

; -------------------------------------------------------------------------------------------------------------------

(defn register-handlers [context]
  (-> context
    (register-handler :init init)
    (register-handler :add-editor add-editor!)
    (register-handler :remove-editor remove-editor!)
    (register-handler :wire-editor wire-editor!)
    (register-handler :editor-set-source set-source)
    (register-handler :editor-run-analysis run-analysis)
    (register-handler :editor-update-layout update-layout)
    (register-handler :editor-parse-source parse-source)
    (register-handler :editor-xform dispatch-xform)
    ;(register-handler :store-editor-undo-snapshot push-undo)
    ;(register-handler :store-editor-redo-snapshot push-redo)
    ;(register-handler :undo undo)
    ;(register-handler :redo redo)
    ))