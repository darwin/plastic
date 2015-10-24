(ns plastic.system.dev
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]])
  (:require [com.stuartsierra.component :refer [system-map using]]
            [plastic.component.env :refer [make-env]]
            [plastic.component.figwheel :refer [make-figwheel]]
            [plastic.component.services :refer [make-services]]
            [plastic.component.main-frame :refer [make-main-frame]]
            [plastic.component.worker-frame :refer [make-worker-frame]]))

; -------------------------------------------------------------------------------------------------------------------

(defn make-dev-system [config services]
  (system-map
    :env (make-env config)
    :services (using (make-services services) [:env])
    :figwheel (using (make-figwheel) [:env])
    :main-frame (using (make-main-frame :main) [:env :services])
    :worker-frame (using (make-worker-frame :worker) [:env :services])))