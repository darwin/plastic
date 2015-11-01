(ns plastic.system
  (:require [plastic.logging :refer-macros [log info warn error group group-end fancy-log]]
            [com.stuartsierra.component :refer [system-map using]]
            [plastic.component.env :refer [make-env]]
            [plastic.component.sonar-pool :refer [make-sonar-pool]]
            [plastic.component.figwheel :refer [make-figwheel]]
            [plastic.component.services :refer [make-services]]
            [plastic.component.main-frame :refer [make-main-frame]]
            [plastic.component.worker-frame :refer [make-worker-frame]]))

; -------------------------------------------------------------------------------------------------------------------

(defn make-system [config services]
  (system-map
    :env (make-env config)
    :sonar-pool (using (make-sonar-pool) [:env])
    :services (using (make-services services) [:env])
    :figwheel (using (make-figwheel) [:env])
    :main-frame (using (make-main-frame :main) [:env :services :sonar-pool])
    :worker-frame (using (make-worker-frame :worker) [:env :services :sonar-pool])))