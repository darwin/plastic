(ns plastic.main.editor.ops
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main.glue :refer [react! dispatch worker-dispatch worker-dispatch-args]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.schema.paths :as paths]
            [plastic.main.editor.ops.editing :as editing]
            [plastic.main.editor.ops.cursor :as cursor]
            [plastic.main.editor.model :as editor]))

(defmulti handle (fn [command & _] command))

; ----------------------------------------------------------------------------------------------------------------

(defmethod handle :nop [_ editor]
  editor)

(defmethod handle :spatial-up [_ editor]
  (-> editor
    editing/stop-editing
    cursor/spatial-up))

(defmethod handle :spatial-down [_ editor]
  (-> editor
    editing/stop-editing
    cursor/spatial-down))

(defmethod handle :spatial-left [_ editor]
  (-> editor
    editing/stop-editing
    cursor/spatial-left))

(defmethod handle :spatial-right [_ editor]
  (-> editor
    editing/stop-editing
    cursor/spatial-right))

(defmethod handle :structural-left [_ editor]
  (-> editor
    editing/stop-editing
    cursor/structural-left))

(defmethod handle :structural-right [_ editor]
  (-> editor
    editing/stop-editing
    cursor/structural-right))

(defmethod handle :structural-up [_ editor]
  (-> editor
    editing/stop-editing
    cursor/structural-up))

(defmethod handle :structural-down [_ editor]
  (let [new-editor (cursor/structural-down editor)]
    (if (identical? new-editor editor)
      (dispatch :editor-op (editor/get-id editor) :toggle-editing))
    new-editor))

(defmethod handle :next-token [_ editor]
  (let [editor-with-moved-selection (-> editor cursor/next-token)
        moved-selection (editor/get-selection editor-with-moved-selection)]
    (-> editor
      editing/stop-editing
      (editor/set-selection moved-selection)
      editing/start-editing)))

(defmethod handle :prev-token [_ editor]
  (let [editor-with-moved-selection (-> editor cursor/prev-token)
        moved-selection (editor/get-selection editor-with-moved-selection)]
    (-> editor
      editing/stop-editing
      (editor/set-selection moved-selection)
      editing/start-editing)))

(defmethod handle :toggle-editing [_ editor]
  (if (editor/editing? editor)
    (editing/stop-editing editor)
    (editing/start-editing editor)))

(defmethod handle :start-editing [_ editor]
  (editing/start-editing editor))

(defmethod handle :stop-editing [_ editor]
  (editing/stop-editing editor))

(defmethod handle :enter [_ editor]
  {:pre [(not (editing/editing-string? editor))]}
  (editing/perform-enter editor))

(defmethod handle :alt-enter [_ editor]
  {:pre [(not (editing/editing-string? editor))]}
  (editing/perform-alt-enter editor))

(defmethod handle :space [_ editor]
  {:pre [(not (editing/editing-string? editor))]}
  (editing/perform-space editor))

(defmethod handle :backspace [_ editor]
  (if (editor/editing? editor)
    (editing/perform-backspace-in-empty-cell editor)
    (editing/perform-backspace editor)))

(defmethod handle :delete [_ editor]
  (editing/delete-linebreak-or-token-after-cursor editor))

(defmethod handle :alt-delete [_ editor]
  (editing/delete-linebreak-or-token-before-cursor editor))

(defmethod handle :open-list [_ editor]
  (editing/open-list editor))

(defmethod handle :open-vector [_ editor]
  (editing/open-vector editor))

(defmethod handle :open-map [_ editor]
  (editing/open-map editor))

(defmethod handle :open-set [_ editor]
  (editing/open-set editor))

(defmethod handle :open-fn [_ editor]
  (editing/open-fn editor))

(defmethod handle :open-meta [_ editor]
  (editing/open-meta editor))

(defmethod handle :open-quote [_ editor]
  (editing/open-quote editor))

(defmethod handle :open-deref [_ editor]
  (editing/open-deref editor))

; ----------------------------------------------------------------------------------------------------------------

(defmethod handle :default [op]
  (error (str "Unknown editor operation '" op "'")))

(defn dispatch-command [editors [editor-id op]]
  (let [old-editor (get editors editor-id)]
    (if-let [new-editor (handle op old-editor)]
      (assoc-in editors [editor-id] new-editor)
      editors)))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-op paths/editors-path dispatch-command)