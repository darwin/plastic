(ns meld.ids
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]))

(defonce node-id! (volatile! 0))

(defn next-node-id! []
  (vswap! node-id! inc))
