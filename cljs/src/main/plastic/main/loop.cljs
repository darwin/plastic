(ns plastic.main.loop
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.onion.core]
            [plastic.cogs.boot.core]
            [plastic.main.editor.core]
            [plastic.cogs.commands.core]
            [plastic.main.frame :refer [main-loop]]
            [plastic.main.db]))

(log "PLASTIC MAIN: ENTERING EVENT LOOP")

; start event processing
(main-loop)