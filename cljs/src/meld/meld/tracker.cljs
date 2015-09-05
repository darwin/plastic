(ns meld.tracker
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.gray-matter :refer [parse-gray-matter]]
            [meld.node :as node]))

(defn get-token-start [frames]
  (first (:offset frames)))

(defn get-token-end [frames]
  (.getLength (:buffer frames)))

(defn matches? [range-start node]
  (>= (:start node) range-start))

(defn is-token-interesting? [token]
  (not (object? token)))                                                                                              ; opening brackets emit empty js-object tokens

(defn split-nodes-at [nodes point]
  (let [groups (group-by (partial matches? point) nodes)]
    [(get groups false '()) (get groups true '())]))

; -------------------------------------------------------------------------------------------------------------------

(defn source-tracker [source reader f]
  (let [frames-atom (.-frames reader)
        buffer (:buffer @frames-atom)
        cur-pos (.getLength buffer)]
    (swap! frames-atom update-in [:offset] conj cur-pos)
    (let [token (f)
          frames @frames-atom
          nodes (:nodes frames)
          token-start (get-token-start frames)
          token-end (get-token-end frames)
          chunk (subs source token-start token-end)
          gray-matter (parse-gray-matter chunk token-start [1 1])                                                     ; TODO: rewrite this by simply skipping white matter
          gray-matter-end (or (:end (last gray-matter)) token-start)
          [baked-nodes children] (split-nodes-at nodes gray-matter-end)
          info {:source (subs source gray-matter-end token-end)
                :start  gray-matter-end
                :end    token-end}
          new-nodes (if (is-token-interesting? token) [(node/make-node token info children)])]
      (swap! frames-atom (fn [frames]
                           (assoc frames :nodes (concat baked-nodes new-nodes))))
      (swap! frames-atom update-in [:offset] pop)
      token)))
