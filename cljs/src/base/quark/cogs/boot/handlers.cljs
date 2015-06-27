(ns quark.cogs.boot.handlers
  (:require [quark.frame.core :refer [register-handler]]
            [quark.schema.paths :as paths])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn init [db [state]]
  (log "init" state)
  db)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :init init)