(ns plastic.main.editor.layout
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.editor.model :as editor]
            [plastic.main.paths :as paths]))

(defn commit-layout [editors [editor-id form-id layout selectables spatial-web structural-web]]
  (let [editor (get editors editor-id)
        new-editor (-> editor
                     (editor/set-layout-for-form form-id layout)
                     (editor/set-selectables-for-form form-id selectables)
                     (editor/set-spatial-web-for-form form-id spatial-web)
                     (editor/set-structural-web-for-form form-id structural-web))]
    (assoc editors editor-id new-editor)))

(defn update-render-state [editors [editor-id render-state]]
  (let [editor (get editors editor-id)
        new-editor (editor/set-render-state editor render-state)]
    (assoc editors editor-id new-editor)))

; ---------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-commit-layout paths/editors-path commit-layout)
(register-handler :editor-update-render-state paths/editors-path update-render-state)