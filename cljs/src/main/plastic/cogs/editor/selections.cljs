(ns plastic.cogs.editor.selections
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]]
                   [plastic.macros.common :refer [*->]])
  (:require [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.schema.paths :as paths]
            [plastic.cogs.editor.layout.utils :refer [apply-to-selected-editors]]
            [plastic.cogs.editor.ops.editing :as editing]))

; ----------------------------------------------------------------------------------------------------------------

(defn clear-all-selections-in-editor [editor]
  (let [last-focused-form-id (get-in editor [:selections :focused-form-id])]
    (-> editor
      (editing/stop-editing)
      (assoc-in [:selections] {:focused-form-id last-focused-form-id}))))

(defn clear-all-selections [editors [editor-selector]]
  (apply-to-selected-editors clear-all-selections-in-editor editors editor-selector))

(defn select-in-editor [form-id selections editor]
  (-> editor
    (editing/stop-editing)
    (assoc-in [:selections] {:focused-form-id form-id
                             form-id          selections})))

(defn select [editors [editor-selector form-id selections]]
  (apply-to-selected-editors (partial select-in-editor form-id selections) editors editor-selector))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-clear-all-selections paths/editors-path clear-all-selections)
(register-handler :editor-select paths/editors-path select)
