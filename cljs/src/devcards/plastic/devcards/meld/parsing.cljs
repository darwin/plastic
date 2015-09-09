(ns plastic.devcards.meld.parsing
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [devcards.core :refer [defcard deftest]]
                   [plastic.devcards :refer [defmeldcard]])
  (:require [meld.parser :as parser]))

;(defmeldcard "0")
;(defmeldcard "\"string\"")
;(defmeldcard "(0 1)")
;(defmeldcard "[0 1 2]")
(defmeldcard "  ; comment")

; something more complex
;(defmeldcard "[0 symbol :keyword \"string\" #\"regex\" [x y z] '(1 2 3) #{3 4 4} {:k v 0 \"x\"}]")




