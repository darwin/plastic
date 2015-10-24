(ns plastic.env
  (:refer-clojure :exclude [get])
  (:require [plastic.globals]
            [plastic.config]))

; -------------------------------------------------------------------------------------------------------------------

(defn get [context name]
  (name (get-in context [:env :config])))