(ns meld.tests.core
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.devcards.util :as util :refer-macros [deftest meld-zip-card]]
            [cljs.test :refer-macros [is testing]]
            [meld.tests.helpers :refer [clip-node nodes-match?]]
            [meld.core :as meld]
            [meld.node :as node]
            [meld.zip :as zip]))

(defn build-tree-based-meld-and-protocol []
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

(defn build-tree-based-meld []
  (first (build-tree-based-meld-and-protocol)))

; -------------------------------------------------------------------------------------------------------------------

(deftest meld-construction
  (testing "construct an empty meld from scratch"
    (let [empty-meld (meld/make {} nil)]
      (is (= (meld/nodes-count empty-meld) 0))
      (is (= (meld/get-root-node-id empty-meld) nil))
      (is (= (meld/get-root-node empty-meld) nil))
      (is (= (meld/get-node empty-meld 0) nil))))
  (testing "construct a simple meld from scratch"
    (let [root-node {:id 1 :kind :root :children [1 2]}
          leaf1-node {:id 2 :kind :leaf}
          leaf2-node {:id 3 :kind :leaf}
          base {1 root-node 2 leaf1-node 3 leaf2-node}
          hand-made-meld (meld/make base 1 4)]
      (is (= (meld/nodes-count hand-made-meld) 3))
      (is (= (meld/get-root-node-id hand-made-meld) 1))
      (is (= (meld/get-root-node hand-made-meld) root-node))
      (is (= (meld/get-node hand-made-meld 1) root-node))
      (is (= (meld/get-node hand-made-meld 2) leaf1-node))
      (is (= (meld/get-node hand-made-meld 3) leaf2-node))
      (is (= (meld/get-node hand-made-meld 4) nil)))))

(meld-zip-card "tree-based-meld" nil #(zip/zip (build-tree-based-meld)))

(deftest tree-made-meld-tests
  (testing "excecise a simple tree-based meld"
    (let [[meld protocol] (build-tree-based-meld-and-protocol)
          {:keys [root vector map symbol]} protocol]
      (is (= (meld/nodes-count meld) 7))
      (is (nodes-match? (meld/get-root-node meld) root))
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
      (is (= (meld/get-root-node-id meld) 1)))))