(ns plastic.main.init
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main.glue :refer [worker-dispatch worker-dispatch-with-effect worker-dispatch-args]])
  (:require [plastic.env]
            [plastic.reagent.core]
            [plastic.onion.core]
            [plastic.main.servant :as servant]
            [plastic.main.schema.core]
            [plastic.main.editor.core]
            [plastic.main.commands]
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