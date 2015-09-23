(ns plastic.devcards.meld.core-tests
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [cljs.test :refer-macros [is testing]]
            [plastic.devcards.util :refer-macros [deftest]]
            [devcards.core]))

(deftest first-testers
  "## This is documentation
   It should work well"
  (testing "good stuff"
    (is (= (+ 3 4 55555) 4) "Testing the adding")
    (is (= (+ 1 0 0 0) 1) "This shouldn't work")
    (is (= 1 3))
    (is (throw "heck")))
  "## And here is more documentation"
  (testing "bad stuff"
    (is (= (+ 1 0 0 0) 1))
    (is (= (+ 3 4 55555) 4))
    (testing "mad stuff"
      (is (= (+ 1 0 0 0) 1))
      (is (= (+ 3 4 55555) 4)))))