(ns plastic.main
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.frame.router :refer [router-loop]]
            [plastic.onion.core]
            [plastic.cogs.boot.core]
            [plastic.cogs.editor.core]
            [plastic.cogs.commands.core]
            [plastic.db]))

(log "===================== PLASTIC ENTERING EVENT LOOP =====================")

; start event processing
(router-loop)