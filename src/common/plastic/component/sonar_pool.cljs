(ns plastic.component.sonar-pool
  (:require [plastic.logging :refer-macros [log info warn error group group-end fancy-log]]
            [com.stuartsierra.component :as component]
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