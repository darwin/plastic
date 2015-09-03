(ns plastic.main.editor.layout
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.editor.model :as editor]
            [plastic.main.paths :as paths]))

(defn commit-layout-patch [editors [editor-selector form-id layout selectables spatial-web structural-web]]
  (editor/apply-to-editors editors editor-selector
    (fn [editor]
      (-> editor
        (editor/set-layout-patch-for-form form-id layout)
        (editor/set-selectables-patch-for-form form-id selectables)
        (editor/set-spatial-web-patch-for-form form-id spatial-web)
        (editor/set-structural-web-patch-for-form form-id structural-web)
        (editor/set-puppets #{})
        (editor/set-highlight #{})))))

(defn update-render-state [editors [editor-selector render-state]]
  (editor/apply-to-editors editors editor-selector
    (fn [editor]
      (editor/set-render-state editor render-state))))

(defn remove-forms [editors [editor-selector form-ids]]
  (editor/apply-to-editors editors editor-selector
    (fn [editor]
      (editor/remove-forms editor form-ids))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-commit-layout-patch paths/editors-path commit-layout-patch)
(register-handler :editor-update-render-state paths/editors-path update-render-state)
(register-handler :editor-remove-forms paths/editors-path remove-forms)
