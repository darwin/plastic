(ns plastic.main.editor.selections
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.schema.paths :as paths]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.ops.editing :as editing]))

; ----------------------------------------------------------------------------------------------------------------

(defn clear-selection-in-editor [editor]
  (-> editor
    (editing/stop-editing)
    (editor/set-selection #{})))

(defn clear-selection [editors [editor-selector]]
  (editor/apply-to-specified-editors clear-selection-in-editor editors editor-selector))

(defn set-selection-in-editor [selection editor]
  (-> editor
    (editing/stop-editing)
    (editor/set-selection selection)))

(defn set-selection [editors [editor-selector selection]]
  (editor/apply-to-specified-editors (partial set-selection-in-editor selection) editors editor-selector))

(defn toggle-selection-in-editor [selection editor]
  (-> editor
    (editing/stop-editing)
    (editor/toggle-selection selection)))

(defn toggle-selection [editors [editor-selector selection]]
  (editor/apply-to-specified-editors (partial toggle-selection-in-editor selection) editors editor-selector))

(defn set-cursor-in-editor [cursor link? editor]
  (-> editor
    (editor/set-cursor cursor link?)))

(defn set-cursor [editors [editor-selector cursor link?]]
  (editor/apply-to-specified-editors (partial set-cursor-in-editor cursor link?) editors editor-selector))

(defn clear-cursor-in-editor [editor]
  (-> editor
    (editing/stop-editing)
    (editor/set-cursor nil)))

(defn clear-cursor [editors [editor-selector]]
  (editor/apply-to-specified-editors clear-cursor-in-editor editors editor-selector))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-clear-selection paths/editors-path clear-selection)
(register-handler :editor-clear-cursor paths/editors-path clear-cursor)
(register-handler :editor-set-selection paths/editors-path set-selection)
(register-handler :editor-toggle-selection paths/editors-path toggle-selection)
(register-handler :editor-set-cursor paths/editors-path set-cursor)