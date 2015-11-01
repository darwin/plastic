(ns meld.tests.helpers
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.devcards.util]
            [cljs.test]
            [meld.zip :as zip]
            [meld.parser :as parser]))

; -------------------------------------------------------------------------------------------------------------------

(defn clip-node [node]
  (select-keys node [:type :tag :source]))

(defn nodes-match? [n1 n2]
  (= (clip-node n1) (clip-node n2)))

(defn zip-from-source [source]
  (zip/zip (parser/parse! source)))

(defn subtree-tags [loc]
  (map zip/get-tag (zip/take-subtree loc)))

(defn children-tags [loc]
  (map zip/get-tag (zip/take-children loc)))
