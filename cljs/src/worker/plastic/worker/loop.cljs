(ns plastic.worker.loop
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker.glue :refer [react! dispatch dispatch-args]])
  (:require [plastic.worker.init]
            [plastic.worker.frame :refer [worker-loop]]))

(log "PLASTIC WORKER: ENTERING EVENT LOOP")

; start event processing
(worker-loop)