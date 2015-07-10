(ns quark.main
  (:require [quark.frame.router :refer [router-loop]]
            [quark.onion.core]
            [quark.cogs.boot.core]
            [quark.cogs.editor.core]
            [quark.cogs.commands.core]
            [quark.db])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(log "===================== QUARK ENTERING EVENT LOOP =====================")

; start event processing
(router-loop)