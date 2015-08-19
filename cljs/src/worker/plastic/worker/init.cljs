(ns plastic.worker.init
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.env]
            [plastic.worker.paths]
            [plastic.worker.subs]
            [plastic.worker.servant]
            [plastic.worker.editor]
            [plastic.worker.db]
            [plastic.worker.frame :refer [register-handler]]))

(defn init [db [_state]]
  db)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :init init)