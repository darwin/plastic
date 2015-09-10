(ns meld.whitespace
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [clojure.zip :as z]
            [meld.zip :as zip]
            [meld.node :as node]))

(defn merge-whitespace [meld]
  (let [start-loc (zip/zip meld)]
    (loop [loc start-loc]
      (if (zip/end? loc)
        (zip/unzip loc)
        (let [node (zip/node loc)]
          (if (node/whitespace? node)
            (recur (zip/next (if-let [right-loc (zip/right loc)]
                               (-> right-loc
                                 (zip/edit node/set-leadspace-subnode node)
                                 (zip/left)
                                 (zip/remove))
                               (-> (zip/up loc)
                                 (zip/edit node/set-trailspace-subnode node)
                                 (zip/find (zip/id loc))
                                 (zip/remove)))))
            (recur (zip/next loc))))))))

