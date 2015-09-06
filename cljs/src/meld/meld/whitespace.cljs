(ns meld.whitespace
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [clojure.zip :as z]
            [meld.zip :as zip]))

(defn inject-whitespace [node whitespace-node]
  (assoc node :whitespace whitespace-node))

(defn merge-whitespace [node]
  (let [root-loc (zip/zipper node)]
    (loop [loc root-loc]
      (if (z/end? loc)
        (z/root loc)
        (let [node (z/node loc)]
          (if (= (:type node) :whitespace)
            (let [prev-loc (z/remove loc)
                  next-loc (z/next prev-loc)
                  _ (assert next-loc "every whitespace node must have a right siblink")
                  _ (assert (not (zip/whitespace-loc? next-loc)) "whitespace must have non-whitespace right siblink")
                  edited-loc (z/edit next-loc inject-whitespace node)]
              (recur (z/next edited-loc)))
            (recur (z/next loc))))))))
