(ns plastic.worker.editor.layout.analysis.defs
  (:require [plastic.util.helpers :as helpers]
            [rewrite-clj.node :as node]
            [plastic.worker.editor.layout.utils :as layout-utils]
            [plastic.util.zip :as zip-utils]
            [clojure.zip :as z]
            [rewrite-clj.zip :as zip]
            [plastic.worker.editor.layout.utils :as utils])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]))

(defn essential-nodes [nodes]
  (filter #(not (or (node/whitespace? %) (node/comment? %))) nodes))

(defn extract-sym-doc [node]
  (let [children (essential-nodes (layout-utils/unwrap-metas (node/children node)))
        first-string-node (first (filter utils/string-node? children))
        first-symbol-node (first (rest (filter utils/symbol-node? children)))]
    [(if first-symbol-node
       [(:id first-symbol-node) {:def-name? true}])
     (if first-string-node
       [(:id first-string-node) {:def-doc? true :selectable? true}])
     [(:id node) {:def?          true
                  ;:def-name-node first-symbol-node
                  ;:def-doc-node  first-string-node
                  }]]))

(defn structure? [loc]
  (let [node (z/node loc)]
    (not (or (node/whitespace? node) (node/comment? node)))))

(def zip-next (partial zip-utils/zip-next structure?))

(defn find-def-locs [loc]
  (filter utils/is-def? (take-while zip-utils/valid-loc? (iterate zip-next loc))))

(defn analyze-defs [analysis loc]
  (let [def-locs (find-def-locs loc)
        defs-analysis (into {} (mapcat (fn [loc] (extract-sym-doc (zip/node loc))) def-locs))]
    (helpers/deep-merge analysis defs-analysis)))