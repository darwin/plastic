(ns plastic.devcards.test
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [devcards.core :refer [defcard deftest]]))

(defcard (list 1 2 3))

(defcard my-first-card
  "Devcards is freaking awesome!")
