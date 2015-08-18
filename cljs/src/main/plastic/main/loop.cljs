(ns plastic.main.loop
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.frame :refer [main-loop main-frame]]
            [plastic.main.init]))

(log "MAIN: ENTERING EVENT LOOP" @main-frame)

; start event processing
(main-loop)
