(ns plastic.cogs.boot.init
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.frame :refer [register-handler]]))

(defn init [db [state]]
  (log "init" state)
  db)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :init init)