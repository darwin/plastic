(ns plastic.main.commands
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.commands.settings :as settings]))

(def handlers
  {:toggle-headers           settings/toggle-headers
   :toggle-code              settings/toggle-code
   :toggle-docs              settings/toggle-docs
   :toggle-selections-debug  settings/toggle-selections-debug})

(defn dispatch-command [db [command & args]]
  (if-let [handler (command handlers)]
    (or (handler db args) db)
    (do
      (error "unimplemented command" command)
      db)))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :command dispatch-command)
