(ns plastic.cogs.editor.layout.analysis.calls
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.node :as node]
            [rewrite-clj.zip :as zip]
            [clojure.zip :as z]
            [plastic.util.zip :as zip-utils]
            [plastic.util.helpers :as helpers]))

(defn structure? [loc]
  (let [node (z/node loc)]
    (not (or (node/whitespace? node) (node/comment? node)))))

(def zip-next (partial zip-utils/zip-next structure?))

(defn is-call? [loc]
  (if-let [parent-loc (z/up loc)]
    (if (= (zip/tag parent-loc) :list)
      (= loc (z/leftmost loc)))))

(defn find-call-locs [loc]
  (filter is-call? (take-while zip-utils/valid-loc? (iterate zip-next loc))))

(defn analyze-calls [analysis loc]
  (let [call-locs (find-call-locs loc)
        call-analysis (into {} (map (fn [loc] [(zip-utils/loc-id loc) {:call? true}]) call-locs))]
    (helpers/deep-merge analysis call-analysis)))
