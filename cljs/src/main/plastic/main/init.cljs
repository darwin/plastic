(ns plastic.main.init
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [worker-dispatch worker-dispatch-with-effect worker-dispatch-args]])
  (:require [plastic.env]
            [plastic.reagent.patch]
            [plastic.onion.atom]
            [plastic.main.schema]
            [plastic.main.editor]
            [plastic.main.commands]
            [plastic.main.servant :as servant]
            [plastic.onion.api :refer [info]]
            [plastic.main.frame :refer [register-handler]]))

(defn init [db [_state]]
  (let [lib-path (.getLibPath info)]
    (assert lib-path)
    (servant/spawn-workers lib-path))
  (worker-dispatch :init)
  db)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :init init)