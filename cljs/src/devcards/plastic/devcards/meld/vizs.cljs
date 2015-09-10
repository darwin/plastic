(ns plastic.devcards.meld.vizs
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [devcards.core :as dc :refer [defcard deftest]]
                   [plastic.devcards :refer [defmeldcard defmeldvizcard defhistcard]])
  (:require [meld.parser :as parser]
            [reagent.core :as r]
            [meld.support :refer [histogram-display]]
            [meld.meld :as meld]
            [meld.zip :as zip]
            [meld.node :as node]))

(defmeldvizcard bad-case (parser/parse! " (0   )  "))

(defmeldvizcard small-vec (parser/parse! "[0 1 2]"))

(defmeldvizcard simple-fn (parser/parse! "(ns n1.simplefn)

(defn fn1 [p1]
  (fn fn2 [p2]
    (use p1 p2)))"))
