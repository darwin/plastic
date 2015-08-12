(ns plastic.cogs.boot.init
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main.glue :refer [worker-dispatch]])
  (:require [plastic.main.frame :refer [register-handler]]
            [plastic.main.servant :refer [spawn-workers]]
            [plastic.onion.api :refer [info]]))

(defn init [db [state]]
  (log "init" state)
  (let [lib-path (.getLibPath info)]
    (assert lib-path)
    (spawn-workers lib-path))
  (worker-dispatch :init)
  db)


; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :init init)