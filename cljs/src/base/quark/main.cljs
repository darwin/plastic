(ns quark.main
  (:require [quark.frame.router :refer [router-loop]]
            [quark.db :refer [app-db]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

; at this point we know init namespace was already required with all deps

(log "===================== QUARK INIT START =====================")

(log "=====================  QUARK INIT END  =====================")

; start event processing
(router-loop)

(log app-db)
