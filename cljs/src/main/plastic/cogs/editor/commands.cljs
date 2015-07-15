(ns plastic.cogs.editor.commands
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]])
  (:require [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.schema.paths :as paths]
            [plastic.cogs.editor.ops.editing :as editing]
            [plastic.cogs.editor.ops.selection :as selection]
            [plastic.cogs.editor.model :as editor]))

(defmulti handle (fn [command & _] command))

; ----------------------------------------------------------------------------------------------------------------

(defmethod handle :spatial-up [_ editor]
  (-> editor
    editing/stop-editing
    selection/spatial-up))

(defmethod handle :spatial-down [_ editor]
  (-> editor
    editing/stop-editing
    selection/spatial-down))

(defmethod handle :spatial-left [_ editor]
  (-> editor
    editing/stop-editing
    selection/spatial-left))

(defmethod handle :spatial-right [_ editor]
  (-> editor
    editing/stop-editing
    selection/spatial-right))

(defmethod handle :structural-left [_ editor]
  (-> editor
    editing/stop-editing
    selection/structural-left))

(defmethod handle :structural-right [_ editor]
  (-> editor
    editing/stop-editing
    selection/structural-right))

(defmethod handle :structural-up [_ editor]
  (-> editor
    editing/stop-editing
    selection/structural-up))

(defmethod handle :structural-down [_ editor]
  (let [new-editor (selection/structural-down editor)]
    (if (= new-editor editor)
      (dispatch :editor-command (editor/get-id editor) :toggle-editing))
    new-editor))

(defmethod handle :next-token [_ editor]
  (-> editor
    editing/stop-editing
    selection/next-token
    editing/start-editing))

(defmethod handle :prev-token [_ editor]
  (-> editor
    editing/stop-editing
    selection/prev-token
    editing/start-editing))

(defmethod handle :toggle-editing [_ editor]
  (if (editor/editing? editor)
    (editing/stop-editing editor)
    (editing/start-editing editor)))

(defmethod handle :start-editing [_ editor]
  (editing/start-editing editor))

(defmethod handle :stop-editing [_ editor]
  (editing/stop-editing editor))

(defmethod handle :enter [_ editor]
  (editing/enter editor))

(defmethod handle :space [_ editor]
  (editing/space editor))

(defmethod handle :delete-selection [_ editor]
  (editing/delete-selection editor))

; ----------------------------------------------------------------------------------------------------------------

(defmethod handle :default [command]
  (error (str "Unknown editor command for dispatch '" command "'")))

(defn dispatch-command [editors [editor-id command]]
  (let [old-editor (get editors editor-id)]
    (if-let [new-editor (handle command old-editor)]
      (assoc-in editors [editor-id] new-editor)
      editors)))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-command paths/editors-path dispatch-command)