(ns plastic.worker.loop
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [react! dispatch dispatch-args]])
  (:require [plastic.worker.init]
            [plastic.worker.frame :refer [worker-loop worker-frame]]))

(log "WORK: ENTERING EVENT LOOP" @worker-frame)

; start event processing
(worker-loop)
