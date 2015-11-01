(ns plastic.api
  (:require [plastic.logging :refer-macros [log info warn error group group-end fancy-log]]
            [plastic.onion.inface :as onion]
            [plastic.core :refer [create-system!]]))

; -------------------------------------------------------------------------------------------------------------------

(defn ^:export boot [& args]
  (apply create-system! args))

(defn ^:export send [system & args]
  (let [context (:main-frame system)]
    (apply onion/send context args)))