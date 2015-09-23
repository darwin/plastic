(ns plastic.devcards.meld.parsing
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.devcards.util :refer-macros [def-meld-data-card]]))

(def card-ns :meld.parsing)

(def-meld-data-card card-ns "a single token" "0")
(def-meld-data-card card-ns "some whitespace around" " (0   )  ")
(def-meld-data-card card-ns "a string" "\"string\"")
(def-meld-data-card card-ns "a list" "(0 1)")
(def-meld-data-card card-ns "a vector" "[0 1 2]")
(def-meld-data-card card-ns "a comment with newline" "; comment \n  ")
(def-meld-data-card card-ns "trailing whitespace" " (1  )   ")
(def-meld-data-card card-ns "something more complex"
  "[0 symbol :keyword \"string\" #\"regex\" [x y z] '(1 2 3) #{3 4 4} {:k v 0 \"x\"}]")



