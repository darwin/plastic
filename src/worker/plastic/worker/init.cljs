(ns plastic.worker.init
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]))

; -------------------------------------------------------------------------------------------------------------------

(defn init [context db [_state]]
  db)