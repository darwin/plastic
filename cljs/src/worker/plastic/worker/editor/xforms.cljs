(ns plastic.worker.editor.xforms
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker.glue :refer [react! dispatch]])
  (:require [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.schema.paths :as paths]
            [plastic.worker.editor.xforms.editing :as editing]))

(defmulti handle (fn [command & _] command))

; ----------------------------------------------------------------------------------------------------------------

(defmethod handle :edit-node [_ editor edit-point value]
  (editing/edit-node editor edit-point value))

(defmethod handle :enter [_ editor edit-point]
  (editing/enter editor edit-point))

(defmethod handle :alt-enter [_ editor edit-point]
  (editing/alt-enter editor edit-point))

(defmethod handle :space [_ editor edit-point]
  (editing/space editor edit-point))

(defmethod handle :backspace [_ editor edit-point]
  (editing/backspace editor edit-point))

(defmethod handle :delete [_ editor edit-point]
  (editing/delete editor edit-point))

(defmethod handle :alt-delete [_ editor edit-point]
  (editing/alt-delete editor edit-point))

(defmethod handle :open-list [_ editor edit-point]
  (editing/open-list editor edit-point))

(defmethod handle :open-vector [_ editor edit-point]
  (editing/open-vector editor edit-point))

(defmethod handle :open-map [_ editor edit-point]
  (editing/open-map editor edit-point))

(defmethod handle :open-set [_ editor edit-point]
  (editing/open-set editor edit-point))

(defmethod handle :open-fn [_ editor edit-point]
  (editing/open-fn editor edit-point))

(defmethod handle :open-meta [_ editor edit-point]
  (editing/open-meta editor edit-point))

(defmethod handle :open-quote [_ editor edit-point]
  (editing/open-quote editor edit-point))

(defmethod handle :open-deref [_ editor edit-point]
  (editing/open-deref editor edit-point))

(defmethod handle :insert-placeholder-as-first-child [_ editor edit-point]
  (editing/insert-placeholder-as-first-child editor edit-point))

; ----------------------------------------------------------------------------------------------------------------

(defmethod handle :default [command]
  (error (str "Unknown editor command for dispatch '" command "'")))

(defn dispatch-command [editors [editor-id command & args]]
  (let [old-editor (get editors editor-id)]
    (if-let [new-editor (apply handle command old-editor args)]
      (assoc-in editors [editor-id] new-editor)
      editors)))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-xform paths/editors-path dispatch-command)