(ns plastic.api
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]])
  (:require [plastic.onion.inface :as onion]
            [plastic.core :refer [create-system!]]))

; -------------------------------------------------------------------------------------------------------------------

(defn ^:export boot [& args]
  (apply create-system! args))

(defn ^:export send [system & args]
  (let [context (:main-frame system)]
    (apply onion/send context args)))