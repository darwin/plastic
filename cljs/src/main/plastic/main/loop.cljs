(ns plastic.main.loop
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]])
  (:require [plastic.main.frame :refer [main-loop main-frame]]
            [plastic.main.init]))

(fancy-log "MAIN LOOP" @main-frame)

; start event processing
(main-loop)
