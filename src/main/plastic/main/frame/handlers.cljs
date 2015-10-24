(ns plastic.main.frame.handlers
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.frame :refer [worker-dispatch worker-dispatch]])
  (:require [plastic.frame :refer [register-handler]]
            [plastic.main.init :refer [init job-done]]
            [plastic.main.commands :refer [dispatch-command]]
            [plastic.main.editor.analysis :refer [commit-analysis-patch]]
            [plastic.main.editor.cursor :refer [clear-cursor set-cursor]]
            [plastic.main.editor.layout :refer [commit-layout-patch update-units remove-units]]
            [plastic.main.editor.lifecycle :refer [add-editor! remove-editor! wire-editor! update-inline-editor]]
            [plastic.main.editor.loader :refer [fetch-text]]
            [plastic.main.editor.ops :refer [dispatch-op]]
            [plastic.main.editor.selection :refer [clear-selection set-selection toggle-selection]]
            [plastic.main.editor.xform :refer [announce-xform]]
            [plastic.undo :refer [push-undo push-redo]]
            [plastic.main.undo :refer [undo redo]]))

; -------------------------------------------------------------------------------------------------------------------

(defn register-handlers [context]
  (-> context
    (register-handler :init init)
    (register-handler :worker-job-done job-done)
    (register-handler :editor-commit-analysis-patch commit-analysis-patch)
    (register-handler :editor-clear-cursor clear-cursor)
    (register-handler :editor-set-cursor set-cursor)
    (register-handler :editor-commit-layout-patch commit-layout-patch)
    (register-handler :editor-update-units update-units)
    (register-handler :editor-remove-units remove-units)
    (register-handler :add-editor add-editor!)
    (register-handler :remove-editor remove-editor!)
    (register-handler :wire-editor wire-editor!)
    (register-handler :editor-update-inline-editor update-inline-editor)
    (register-handler :editor-fetch-text fetch-text)
    (register-handler :editor-op dispatch-op)
    (register-handler :editor-clear-selection clear-selection)
    (register-handler :editor-set-selection set-selection)
    (register-handler :editor-toggle-selection toggle-selection)
    (register-handler :editor-announce-xform announce-xform)
    (register-handler :command dispatch-command)
    ;(register-handler :store-editor-undo-snapshot push-undo)
    ;(register-handler :store-editor-redo-snapshot push-redo)
    ;(register-handler :undo undo)
    ;(register-handler :redo redo)
    ))