(ns plastic.cogs.commands.core
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.frame :refer [subscribe register-handler]]
            [plastic.schema.paths :as paths]
            [plastic.cogs.commands.utils :refer [toggle-setting]]))

(defmulti handle (fn [command & _] command))

; ----------------------------------------------------------------------------------------------------------------

(defmethod handle :toggle-headers [_ settings]
  (toggle-setting settings :headers-visible))

(defmethod handle :toggle-code [_ settings]
  (toggle-setting settings :code-visible))

(defmethod handle :toggle-docs [_ settings]
  (toggle-setting settings :docs-visible))

(defmethod handle :toggle-render-tree-debug [_ settings]
  (toggle-setting settings :render-tree-debug-visible))

(defmethod handle :toggle-parser-debug [_ settings]
  (toggle-setting settings :parser-debug-visible))

(defmethod handle :toggle-text-input-debug [_ settings]
  (toggle-setting settings :text-input-debug-visible))

(defmethod handle :toggle-text-output-debug [_ settings]
  (toggle-setting settings :text-output-debug-visible))

(defmethod handle :toggle-selections-debug [_ settings]
  (toggle-setting settings :selections-debug-visible))

; ----------------------------------------------------------------------------------------------------------------

(defmethod handle :default [command]
  (error (str "Unknown command for dispatch '" command "'"))
  nil)

(defn dispatch-command [settings [command]]
  (if-let [new-settings (handle command settings)]
    new-settings
    settings))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :command paths/settings-path dispatch-command)