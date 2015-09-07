(ns meld.whitespace
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [clojure.zip :as z]
            [meld.zip :as zip]
            [meld.node :as node]))

(defn inject-whitespace [node whitespace-node]
  (assoc node :whitespace whitespace-node))

(defn merge-whitespace [meld]
  (let [start-loc (zip/zip meld)]
    (loop [loc start-loc]
      (if (zip/end? loc)
        (zip/unzip loc)
        (let [node (zip/node loc)]
          (if (= (node/get-type node) :whitespace)
            (let [prev-loc (zip/remove loc)
                  next-loc (zip/next prev-loc)
                  _ (assert next-loc "every whitespace node must have a right siblink")
                  _ (assert (not (node/whitespace? (zip/node next-loc))) "whitespace must have non-whitespace right siblink")
                  edited-loc (zip/edit next-loc inject-whitespace node)]
              (recur (zip/next edited-loc)))
            (recur (zip/next loc))))))))

