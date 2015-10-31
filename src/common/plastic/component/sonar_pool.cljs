(ns plastic.component.sonar-pool
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]])
  (:require [com.stuartsierra.component :as component]
            [plastic.reagent.sonar :refer [init-sonar-pool clean-sonar-pool]]
            [plastic.main.frame :refer [start stop]]))

; -------------------------------------------------------------------------------------------------------------------

(defrecord SonarPool []

  component/Lifecycle
  (start [component]
    (-> component
      (init-sonar-pool)))
  (stop [component]
    (-> component
      (clean-sonar-pool))))

(defn make-sonar-pool []
  (SonarPool.))