(ns plastic.component.figwheel
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]])
  (:require [com.stuartsierra.component :as component]
            [plastic.dev.figwheel :refer [start stop]]))

; -------------------------------------------------------------------------------------------------------------------

(defrecord Figwheel []

  component/Lifecycle
  (start [component]
    (start component))
  (stop [component]
    (stop component)))

(defn make-figwheel []
  (Figwheel.))