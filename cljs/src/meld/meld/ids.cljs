(ns meld.ids
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [clojure.zip :as z]
            [meld.zip :as zip]))

(defonce node-id (atom 0))

(defn next-node-id! []
  (swap! node-id inc))

; -------------------------------------------------------------------------------------------------------------------

(defn assign-unique-ids! [node]
  (let [root-loc (zip/make-zipper node)]
    (loop [loc root-loc]
      (if (z/end? loc)
        (z/root loc)
        (let [edited-loc (z/edit loc assoc :id (next-node-id!))]
          (recur (z/next edited-loc)))))))
