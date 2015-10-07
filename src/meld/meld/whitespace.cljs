(ns meld.whitespace
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.zip :as zip]
            [meld.node :as node]))

(defn merge-whitespace [meld]
  (let [start-loc (zip/zip meld)]
    (loop [loc start-loc]
      (if (zip/end? loc)
        (zip/unzip loc)
        (let [node (zip/get-node loc)
              modified-loc (if (node/whitespace? node)
                             (if-let [right-loc (zip/right loc)]                                                      ; first try to assign whitespace to right sibling as leadspace
                               (-> right-loc
                                 (zip/edit node/set-leadspace-subnode node)
                                 (zip/left)
                                 (zip/remove))
                               (-> (zip/up loc)                                                                       ; in case there is no right sibling, assing it to parent as trailspace
                                 (zip/edit node/set-trailspace-subnode node)
                                 (zip/find (zip/get-id loc))
                                 (zip/remove))))]
          (recur (zip/next (or modified-loc loc))))))))
