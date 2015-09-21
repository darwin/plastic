(ns plastic.devcards.meld.zip
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.devcards.util :refer [def-zip-card]]
            [meld.zip :as z]))

(def card-ns :meld.zip)

(def-zip-card card-ns "empty file" "")

(def-zip-card card-ns "a single token" "0")

(def-zip-card card-ns "more interesting example" "(0 1 ; a comment\n[2 3])"
  #(-> % (z/down) (z/down) (z/down) (z/right) (z/right) (z/right) (z/right) (z/subzip) (z/down)))

(def-zip-card card-ns "something more complex"
  (str
    "[0 symbol :keyword \"string\" #\"regex\""
    "[x y z] '(1 2 3) #{3 4 4} {:k v 0 \"x\"}]"))

(def-zip-card card-ns "multiple units" "(def form 1) ; with comment\n\n; a standalone comment\n\n(second-form)")
