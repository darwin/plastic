(ns plastic.worker.init
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.frame :refer [main-dispatch]]))

; -------------------------------------------------------------------------------------------------------------------

(defn init [context db [_state]]
  db)