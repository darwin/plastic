(ns plastic.core
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]])
  (:require [com.stuartsierra.component :as component]
            [plastic.globals :as globals]
            [plastic.system :refer [make-system]]
            [plastic.onion.api :refer [expose-global-apis!]]
            [plastic.util.helpers :refer [convert-from-js]]
            [plastic.env :as env]))

; -------------------------------------------------------------------------------------------------------------------
; plastic.env has been already included externally
; plastic.node has been optionally included externally
; plastic.dev has been optionally included as well
; see boot.coffee for more details

; unfortunately we need cyclic dependency between main and worker component
; to be able send messages to each other without using global references,
; we wire them here as first step after starting the system
(defn wire-components! [system]
  (let [main-frame (:main-frame system)
        worker-frame (:worker-frame system)]
    (vreset! (:main-context worker-frame) main-frame)
    (vreset! (:worker-context main-frame) worker-frame))
  system)

(defn expose-debug-var [name value]
  (aset js/window name value))

(defn expose-debug-vars! [system]
  (expose-debug-var "$system" system)
  (expose-debug-var "$config" (get-in system [:env :config]))
  (expose-debug-var "$fm" (get-in system [:main-frame]))
  (expose-debug-var "$fw" (get-in system [:worker-frame]))
  (expose-debug-var "$dbm" (get-in system [:main-frame :db]))
  (expose-debug-var "$dbw" (get-in system [:worker-frame :db]))
  (expose-debug-var "$services" (get-in system [:services])))

(defn create-system! [js-env js-services]
  (let [env (convert-from-js js-env)
        _ (log "active config" env)
        services (convert-from-js js-services)
        system (-> (make-system env services)
                 (component/start)
                 (wire-components!))]
    (expose-global-apis! js-services)
    (if (env/get system :dev-mode)
      (expose-debug-vars! system))
    (set! globals/*system* system)
    system))