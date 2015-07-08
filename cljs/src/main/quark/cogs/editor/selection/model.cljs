(ns quark.cogs.editor.selection.model
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defmulti op (fn [op-type & _] op-type))

; ----------------------------------------------------------------------------------------------------------------

(defmethod op :move-down [_ cursel all-selectables root-node nodes]
  (let [sel-node-id (first cursel)]))

; ----------------------------------------------------------------------------------------------------------------

(defmethod op :default [command]
  (error (str "Unknown selection operation '" command "'"))
  nil)