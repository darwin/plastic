(ns meld.core-tests
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.devcards.util :as util :refer-macros [deftest def-zip-card]]
            [cljs.test :refer-macros [is testing]]
            [meld.core :as meld]
            [meld.node :as node]
            [meld.zip :as zip]))

(defn prepare-tree-based-meld []
  (let [root-node (node/set-source (node/make-list) "fake source")                                                    ; id 1
        string-node (node/make-string "\"a string\"")                                                                 ; id 2
        linebreak-node (node/make-linebreak)                                                                          ; id 3
        vector-node (node/make-vector)                                                                                ; id 4
        keyword-node (node/make-keyword ":keyword")                                                                   ; id 5
        map-node (node/make-map)                                                                                      ; id 6
        symbol-node (node/make-symbol "a symbol")                                                                     ; id 7
        tree [root-node
              [string-node linebreak-node [vector-node
                                           [keyword-node map-node symbol-node]]]]
        meld (meld/flatten-tree tree)
        protocol {:root      root-node
                  :string    string-node
                  :linebreak linebreak-node
                  :vector    vector-node
                  :keyword   keyword-node
                  :map       map-node
                  :symbol    symbol-node
                  :tree      tree}]
    [meld protocol]))

(defn clip-node [node]
  (select-keys node [:id :type :tag :source]))

(defn nodes-match? [n1 n2]
  (= (clip-node n1) (clip-node n2)))

(def tree-based-result (util/with-stable-meld-ids prepare-tree-based-meld))
(def tree-based-meld (first tree-based-result))
(def tree-based-protocol (second tree-based-result))

; -------------------------------------------------------------------------------------------------------------------

(deftest meld-construction

  (testing "construct an empty meld from scratch"
    (let [empty-meld (meld/make)]
      (is (= (meld/nodes-count empty-meld) 0))
      (is (= (meld/get-top-node-id empty-meld) nil))
      (is (= (meld/get-top-node empty-meld) nil))
      (is (= (meld/get-node empty-meld 0) nil))))

  (testing "construct a simple meld from scratch"
    (let [root-node {:id 0 :kind :root :children [1 2]}
          leaf1-node {:id 1 :kind :leaf}
          leaf2-node {:id 2 :kind :leaf}
          hand-made-meld (meld/make {0 root-node
                                     1 leaf1-node
                                     2 leaf2-node} 0)]
      (is (= (meld/nodes-count hand-made-meld) 3))
      (is (= (meld/get-top-node-id hand-made-meld) 0))
      (is (= (meld/get-top-node hand-made-meld) root-node))
      (is (= (meld/get-node hand-made-meld 0) root-node))
      (is (= (meld/get-node hand-made-meld 1) leaf1-node))
      (is (= (meld/get-node hand-made-meld 2) leaf2-node))
      (is (= (meld/get-node hand-made-meld 3) nil)))))

(def-zip-card :meld.core_tests "tree-based-meld" nil (zip/zip tree-based-meld))

(deftest tree-made-meld-tests
  (testing "excecise a simple tree-based meld"
    (let [meld tree-based-meld
          {:keys [root vector map symbol]} tree-based-protocol]
      (is (= (meld/nodes-count meld) 7))
      (is (nodes-match? (meld/get-top-node meld) root))
      (is (nodes-match? (meld/get-node meld 4) vector))
      (is (nodes-match? (meld/get-node meld 6) map))
      (is (= (meld/get-source meld) (:source root)))
      (is (= (meld/descendants meld 1) '(2 3 4 5 6 7)))
      (is (= (meld/descendants meld 4) '(5 6 7)))
      (is (= (meld/descendants meld 2) '()))
      (is (= (meld/descendants meld 6) '()))
      (is (= (meld/ancestors meld 6) '(4 1)))
      (is (= (meld/ancestors meld 2) '(1)))
      (is (= (meld/ancestors meld 1) '()))
      (is (= (meld/get-top-node-id meld) (node/get-id root))))))