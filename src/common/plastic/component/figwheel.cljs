(ns plastic.component.figwheel
  (:require [plastic.logging :refer-macros [log info warn error group group-end fancy-log]]
            [com.stuartsierra.component :as component]
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