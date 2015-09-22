(ns meld.core-tests
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [cljs.test :refer [deftest is testing]])
  (:require [meld.core :as core]))

(deftest meld-core-sample
  (testing "testing meld"
    (is (= 1 9))))
