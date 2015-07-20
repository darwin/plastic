(ns plastic.cogs.boot.init
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.schema.frame :refer [register-handler]]))

(defn init [db [state]]
  (log "init" state)
  db)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :init init)