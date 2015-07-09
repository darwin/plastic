(ns quark.cogs.editor.commands
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [quark.cogs.editor.editing :as editing]
            [quark.cogs.editor.selections :as selections])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]))

(defmulti handle (fn [command & _] command))

; ----------------------------------------------------------------------------------------------------------------

(defmethod handle :move-up [_ editor]
  (selections/move-up editor))

(defmethod handle :move-down [_ editor]
  (selections/move-down editor))

(defmethod handle :move-left [_ editor]
  (selections/move-left editor))

(defmethod handle :move-right [_ editor]
  (selections/move-right editor))

(defmethod handle :level-up [_ editor]
  (selections/level-up editor))

(defmethod handle :level-down [_ editor]
  (let [new-editor (selections/level-down editor)]
    (if (= new-editor editor)
      (dispatch :editor-command (:id editor) :toggle-editing))
    new-editor))

(defmethod handle :toggle-editing [_ editor]
  (if (empty? (:editing editor))
    (editing/start-editing editor)
    (editing/stop-editing editor)))

(defmethod handle :start-editing [_ editor]
  (editing/start-editing editor))

(defmethod handle :stop-editing [_ editor]
  (editing/stop-editing editor))

; ----------------------------------------------------------------------------------------------------------------

(defmethod handle :default [command]
  (error (str "Unknown editor command for dispatch '" command "'"))
  nil)

(defn dispatch-command [editors [editor-id command]]
  (let [old-editor (get editors editor-id)
        _ (assert old-editor)
        new-editor (handle command old-editor)]
    (if new-editor
      (assoc-in editors [editor-id] new-editor)
      editors)))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-command paths/editors-path dispatch-command)