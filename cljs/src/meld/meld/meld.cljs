(ns meld.meld
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.node :as node]))

(defn descendants [meld id]
  (let [node (get meld id)
        children (node/get-children node)]
    (if (seq children)
      (concat children (apply concat (map (partial descendants meld) children)))
      '())))

(defn dissoc-all! [meld* ids]
  (reduce dissoc! meld* ids))
