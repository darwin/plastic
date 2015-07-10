(ns plastic.cogs.boot.init
  (:require [plastic.frame.core :refer [register-handler]]
            [plastic.schema.paths :as paths])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

(defn init [db [state]]
  (log "init" state)
  db)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :init init)