(ns quark.cogs.commands.core
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [quark.cogs.commands.utils :refer [toggle-setting]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defmulti handle (fn [command & _] command))

; ----------------------------------------------------------------------------------------------------------------

(defmethod handle :toggle-code [_ settings]
  (toggle-setting settings :code-visible))

(defmethod handle :toggle-docs [_ settings]
  (toggle-setting settings :docs-visible))

(defmethod handle :toggle-docs-debug [_ settings]
  (toggle-setting settings :docs-debug-visible))

(defmethod handle :toggle-code-debug [_ settings]
  (toggle-setting settings :code-debug-visible))

(defmethod handle :toggle-parser-debug [_ settings]
  (toggle-setting settings :parser-debug-visible))

(defmethod handle :toggle-plaintext-debug [_ settings]
  (toggle-setting settings :plaintext-debug-visible))

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