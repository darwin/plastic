(ns plastic.worker.loop
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker.glue :refer [react! dispatch dispatch-args]])
  (:require [plastic.worker.servant]
            [plastic.cogs.boot.core]
            [plastic.worker.editor.core]
            [plastic.cogs.commands.core]
            [plastic.worker.frame :refer [worker-loop]]
            [plastic.worker.db]))

(log "PLASTIC WORKER: ENTERING EVENT LOOP")

; start event processing
(worker-loop)