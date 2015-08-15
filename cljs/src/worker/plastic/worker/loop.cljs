(ns plastic.worker.loop
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [react! dispatch dispatch-args]])
  (:require [plastic.worker.init]
            [plastic.worker.frame :refer [worker-loop]]))

(log "WORK: ENTERING EVENT LOOP")

; start event processing
(worker-loop)