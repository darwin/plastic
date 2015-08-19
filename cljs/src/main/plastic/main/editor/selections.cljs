(ns plastic.main.editor.selections
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.schema.paths :as paths]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.ops.editing :as editing]))

; ----------------------------------------------------------------------------------------------------------------------

(defn clear-selection [editors [selector]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (editing/stop-editing editor #(editor/set-selection % #{})))))

(defn set-selection [editors [selector selection]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (editing/stop-editing editor #(editor/set-selection % selection)))))

(defn toggle-selection [editors [selector selection]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (editing/stop-editing editor #(editor/toggle-selection % selection)))))

(defn set-cursor [editors [selector cursor link?]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (editing/stop-editing editor #(editor/set-cursor % cursor link?)))))

(defn clear-cursor [editors [selector]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (editing/stop-editing editor #(editor/set-cursor % nil)))))

; ----------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-clear-selection paths/editors-path clear-selection)
(register-handler :editor-clear-cursor paths/editors-path clear-cursor)
(register-handler :editor-set-selection paths/editors-path set-selection)
(register-handler :editor-toggle-selection paths/editors-path toggle-selection)
(register-handler :editor-set-cursor paths/editors-path set-cursor)