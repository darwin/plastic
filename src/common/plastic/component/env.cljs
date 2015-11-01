(ns plastic.component.env
  (:require [plastic.logging :refer-macros [log info warn error group group-end fancy-log]]
            [com.stuartsierra.component :as component]))

; -------------------------------------------------------------------------------------------------------------------

(defrecord Env [config]

  component/Lifecycle
  (start [component]
    (assoc component :config config))
  (stop [component]
    (dissoc component :config)))

(defn make-env [config]
  (Env. config))