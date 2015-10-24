(ns plastic.test.karma
  (:require [cljs.analyzer.api :as ana-api]))

; -------------------------------------------------------------------------------------------------------------------

(defn public-tests [namespace]
  (filter #(:test (nth % 1) false) (ana-api/ns-publics namespace)))

(defn number-of-tests-in-namespace [namespace]
  (count (public-tests namespace)))

(defn total-number-of-tests-in-namespaces [namespaces]
  (let [counts (map number-of-tests-in-namespace namespaces)]
    (apply + counts)))

; -------------------------------------------------------------------------------------------------------------------

(defmacro total-number-of-tests-in-all-namespaces []
  (total-number-of-tests-in-namespaces (ana-api/all-ns)))