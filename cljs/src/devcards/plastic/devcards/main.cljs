(ns plastic.devcards.main
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [devcards.core :refer [defcard deftest]])
  (:require [plastic.dev.figwheel]))

(defcard (list 1 2 3))

(defcard my-first-card
  "Devcards is freaking awesome!")

(defonce observed-atom
  (let [a (atom 0)]
    (js/setInterval (fn [] (swap! observed-atom inc)) 1000)
    a))

(defcard atom-observing-card observed-atom)
