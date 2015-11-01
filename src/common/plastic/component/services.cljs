(ns plastic.component.services
  (:require [plastic.logging :refer-macros [log info warn error group group-end fancy-log]]
            [com.stuartsierra.component :as component]))

; -------------------------------------------------------------------------------------------------------------------

(defrecord Services [services]

  component/Lifecycle
  (start [component]
    (assoc component :services services))
  (stop [component]
    (dissoc component :services)))

(defn make-services [services]
  (Services. services))