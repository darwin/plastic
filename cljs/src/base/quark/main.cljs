(ns quark.main
  (:require [quark.frame.router :refer [router-loop]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

; at this point we know init namespace was already required with all deps

(log "===================== APP INIT START =====================")
;(app/main)
(log "=====================  APP INIT END  =====================")

; start event processing
(router-loop)
