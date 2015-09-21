(ns plastic.devcards.meld.zip
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [devcards.core :refer [defcard defcard* deftest reagent]])
  (:require [plastic.devcards.util :refer [def-zipviz-card]]
            [meld.zip :as z]))

(def card-ns :meld.zip)

(def-zipviz-card card-ns "empty file" "")

(def-zipviz-card card-ns "a single token" "0")

(def-zipviz-card card-ns "more interesting example" "(0 1 ; a comment\n[2 3])"
  #(-> % (z/down) (z/down) (z/down) (z/right) (z/right) (z/right) (z/right) (z/subzip) (z/down)))

(def-zipviz-card card-ns "something more complex"
  (str
    "[0 symbol :keyword \"string\" #\"regex\""
    "[x y z] '(1 2 3) #{3 4 4} {:k v 0 \"x\"}]"))