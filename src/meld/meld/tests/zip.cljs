(ns meld.tests.zip
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.tests.helpers :refer-macros [simple-zip-test] :refer [zip-from-source subtree-tags children-tags]]
            [cljs.test :refer-macros [is testing]]
            [meld.zip :as z]))

(simple-zip-test "empty-file" #(zip-from-source "")
  (is (= (subtree-tags selected-loc) [:file])))

(simple-zip-test "single-token" #(zip-from-source "t")
  (is (= (subtree-tags selected-loc) [:file :unit :symbol])))

(simple-zip-test "more-interesting" #(-> (zip-from-source "(0 1 ; a comment\n[2 3])")
                                      (z/down) (z/down) (z/down)
                                      (z/right) (z/right) (z/right) (z/right)
                                      (z/subzip) (z/down))
  (is (= (subtree-tags selected-loc) [:number])))

(simple-zip-test "something-more-complex"
  #(-> (zip-from-source "[0 symbol :keyword \"string\" #\"regex\" [x y z] '(1 2 3) #{3 4 4} {:k v 0 \"x\"}]")
    (z/down) (z/down))
  (is (= (children-tags selected-loc) [:number :symbol :kgoiieyword :string :regexp :vector :quote :set :map])))

(simple-zip-test "multiple-units"
  #(zip-from-source "(def form 1) ; with comment\n\n; a standalone comment\n\n(second-form)")
  (is (= (children-tags selected-loc) (repeat 5 :unit))))