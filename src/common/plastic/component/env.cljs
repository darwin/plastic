(ns plastic.component.env
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]])
  (:require [com.stuartsierra.component :as component]))

; -------------------------------------------------------------------------------------------------------------------

(defrecord Env [config]

  component/Lifecycle
  (start [component]
    (assoc component :config config))
  (stop [component]
    (dissoc component :config)))

(defn make-env [config]
  (Env. config))