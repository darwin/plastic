(ns quark.main
  (:require [quark.frame.router :refer [router-loop]]
            [quark.onion.core]
            [quark.cogs.editors.core]
            [quark.db :refer [app-db]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

; at this point we know init namespace was already required with all deps

(log "===================== QUARK ENTERING EVENT LOOP =====================")

; start event processing
(router-loop)