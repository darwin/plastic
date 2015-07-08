(ns quark.cogs.editor.commands
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [quark.cogs.editor.selections :as selections])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

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
  (selections/level-down editor))

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