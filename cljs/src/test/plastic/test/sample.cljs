(ns plastic.test.sample
  (:require [cljs.test :refer-macros [deftest is testing]]))

(deftest sample
  (testing "testing sample"
    (is (= 1 2))))

(deftest sample2
  (testing "testing level 1"
    (testing "testing level 2"
      (testing "testing level 3"
        (is (= 1 3))))))
