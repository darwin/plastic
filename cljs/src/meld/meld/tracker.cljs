(ns meld.tracker
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.gray-matter :refer [measure-gray-matter]]
            [meld.node :as node]))

(defn get-token-start [frames]
  (first (:offset frames)))

(defn get-token-end [frames]
  (.getLength (:buffer frames)))

(defn matches? [range-start node]
  (>= (:start node) range-start))

(defn ^boolean is-token-interesting? [token]
  (not (object? token)))                                                                                              ; opening brackets emit empty js-object tokens

(defn split-nodes-at [nodes point]
  (let [groups (group-by (partial matches? point) nodes)]
    [(get groups false '()) (get groups true '())]))

; -------------------------------------------------------------------------------------------------------------------

(defn source-tracker [source reader f]
  (let [frames-atom (.-frames reader)]
    (swap! frames-atom update-in [:offset] conj (.getLength (:buffer @frames-atom)))
    (let [token (f)]                                                                                                  ; let reader do its hard work... yielding a new token
      (if (is-token-interesting? token)
        (let [frames @frames-atom
              nodes (:nodes frames)
              token-start (get-token-start frames)
              token-end (get-token-end frames)
              chunk (subs source token-start token-end)                                                               ; reader includes comments as part of token start-end range
              gray-matter-end (+ token-start (measure-gray-matter chunk))                                             ; we want to start after potential comments and get meat only
              [baked-nodes children] (split-nodes-at nodes gray-matter-end)                                           ; source tracker is being called depth-first, we collect children from previous calls
              info {:source (subs source gray-matter-end token-end)
                    :start  gray-matter-end
                    :end    token-end}
              new-node (node/make-node-from-token token info children)]                                               ; attach children to new node
          (swap! frames-atom assoc :nodes (conj baked-nodes new-node))))
      (swap! frames-atom update-in [:offset] pop)
      token)))
