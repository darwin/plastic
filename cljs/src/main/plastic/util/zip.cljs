(ns plastic.util.zip
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [reagent.ratom]
            [clojure.zip :as z]))

(defn valid-loc? [loc]
  (not (or (nil? loc) (z/end? loc))))

(defn children-locs [loc]
  (take-while valid-loc? (iterate z/right (z/down loc))))

(defn zip-seq [loc]
  (tree-seq z/branch? children-locs loc))

(defn zip-node-seq [loc]
  (map z/node (zip-seq loc)))
