(ns plastic.component.services
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]])
  (:require [com.stuartsierra.component :as component]))

; -------------------------------------------------------------------------------------------------------------------

(defrecord Services [services]

  component/Lifecycle
  (start [component]
    (assoc component :services services))
  (stop [component]
    (dissoc component :services)))

(defn make-services [services]
  (Services. services))