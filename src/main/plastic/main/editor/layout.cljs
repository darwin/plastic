(ns plastic.main.editor.layout
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.frame :refer [register-handler]]
            [plastic.main.editor.model :as editor]
            [plastic.main.paths :as paths]))

(defn commit-layout-patch [editors [editor-selector unit-id layout-patch spatial-web-patch structural-web-patch]]
  (editor/apply-to-editors editors editor-selector
    (fn [editor]
      (-> editor
        (editor/set-layout-patch-for-unit unit-id layout-patch)
        (editor/set-spatial-web-patch-for-unit unit-id spatial-web-patch)
        (editor/set-structural-web-patch-for-unit unit-id structural-web-patch)
        (editor/set-puppets #{})
        (editor/set-highlight #{})))))

(defn remove-units [editors [editor-selector unit-ids]]
  (editor/apply-to-editors editors editor-selector editor/remove-units unit-ids))

(defn update-units [editors [editor-selector units]]
  (editor/apply-to-editors editors editor-selector editor/set-units units))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-commit-layout-patch paths/editors-path commit-layout-patch)
(register-handler :editor-update-units paths/editors-path update-units)
(register-handler :editor-remove-units paths/editors-path remove-units)
