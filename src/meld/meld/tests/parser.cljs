(ns meld.tests.parser
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.tests.helpers :refer-macros [simple-meld-test]]))

(simple-meld-test "vector-of-numbers" "[1 2 3]" [:file :unit :vector :number :number :number])
(simple-meld-test "vector-of-symbols" "[a b]" [:file :unit :vector :symbol :symbol])
(simple-meld-test "emptry-vector" "[]" [:file :unit :vector])

(simple-meld-test "meta-keyword" "^:dynamic x" [:file :unit :meta :keyword :symbol])

(simple-meld-test "quote-symbol" "'x" [:file :unit :quote :symbol])

(simple-meld-test "deref-symbol" "@x" [:file :unit :deref :symbol])