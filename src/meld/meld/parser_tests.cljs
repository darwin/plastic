(ns meld.parser-tests
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.devcards.util :refer-macros [deftest def-zip-card]]
            [cljs.test :refer-macros [is testing]]
            [meld.zip :as zip]
            [meld.parser :as parser]))

(defn all-tags [meld]
  (map zip/get-tag (zip/take-all zip/next (zip/zip meld))))

(def xvector-meld (parser/parse! "[1 2 3 4 5 6 8]"))

(log "new x" (all-tags xvector-meld) (count (all-tags xvector-meld)))

(def-zip-card :meld.parser_tests "xvector-meld" nil (zip/zip xvector-meld))


(deftest xvector-meld-tests
  (testing "excecise a simple vector meld"
    (let [meld xvector-meld]
      (is (= (all-tags meld) '(:file :unit :vector :symbol :symbol))))))


; -------------------------------------------------------------------------------------------------------------------

(def vector-meld (parser/parse! "[a b]"))
(def-zip-card :meld.parser_tests "vector-meld" nil (zip/zip vector-meld))
(deftest vector-meld-tests
  (testing "excecise a simple vector meld"
    (let [meld vector-meld]
      (is (= (all-tags meld) '(:file :unit :vector :symbol :symbol))))))

; -------------------------------------------------------------------------------------------------------------------

(def meta-meld (parser/parse! "^:dynamic x"))
(def-zip-card :meld.parser_tests "meta-meld" nil (zip/zip meta-meld))
(deftest meta-meld-tests
  (testing "excecise a simple meta meld"
    (let [meld meta-meld]
      (is (= (all-tags meld) '(:file :unit :meta :keyword :symbol))))))

; -------------------------------------------------------------------------------------------------------------------

(def quote-meld (parser/parse! "'x"))
(def-zip-card :meld.parser_tests "quote-meld" nil (zip/zip quote-meld))
(deftest quote-meld-tests
  (testing "excecise a simple quote meld"
    (let [meld quote-meld]
      (is (= (all-tags meld) '(:file :unit :quote :symbol))))))

; -------------------------------------------------------------------------------------------------------------------

(def deref-meld (parser/parse! "@x"))
(def-zip-card :meld.parser_tests "deref-meld" nil (zip/zip deref-meld))
(deftest deref-meld-tests
  (testing "excecise a simple deref meld"
    (let [meld deref-meld]
      (is (= (all-tags meld) '(:file :unit :deref :symbol))))))