(ns plastic.devcards
  (:require [devcards.core]
            [plastic.env]
            [plastic.dev]))

; -------------------------------------------------------------------------------------------------------------------
; this explicit require is here ensure proper order
; clojurescript's :require does not guarantee same order as declared
(.require js/goog "plastic.devcards.all")