(ns plastic.worker.loop
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.worker :refer [react! dispatch dispatch-args]])
  (:require [plastic.worker.init]
            [plastic.worker.frame :refer [worker-loop frame]]))

(fancy-log "WORK LOOP" @frame)
(worker-loop)                                                                                                         ; start event processing
