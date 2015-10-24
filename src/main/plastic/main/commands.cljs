(ns plastic.main.commands
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.commands.settings :as settings]))

; -------------------------------------------------------------------------------------------------------------------

(def handlers
  {:toggle-headers          settings/toggle-headers
   :toggle-code             settings/toggle-code
   :toggle-docs             settings/toggle-docs
   :toggle-comments         settings/toggle-comments
   :toggle-selections-debug settings/toggle-selections-debug})

(defn dispatch-command [context db [command & args]]
  (if-let [handler (command handlers)]
    (or (handler db args) db)
    (do
      (error "unimplemented command" command)
      db)))
