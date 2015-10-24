(ns meld.tracker
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.gray-matter :refer [measure-gray-matter]]
            [meld.node :as node]
            [meld.ids :as ids]
            [meld.core :as meld]
            [meld.util :refer [update!]]))

; -------------------------------------------------------------------------------------------------------------------

(def eof-sentinel ::eof-sentinel)

(defn ^boolean is-token-interesting? [token]
  (not (or (object? token) (keyword-identical? token eof-sentinel))))                                                 ; opening brackets emit empty js-object tokens

(defn find-nodes-after-offset [starts offset]
  (map second (drop-while (fn [[start-offset _id]] (< start-offset offset)) starts)))

(defn filter-nodes-without-parent [meld ids]
  (remove #(node/get-parent (meld/get-node meld %)) ids))

(defn assign-parent! [meld& ids parent]
  (let [* (fn [meld& id] (update! meld& id node/set-parent parent))]
    (reduce * meld& ids)))

; -------------------------------------------------------------------------------------------------------------------

(defn make-tracker [source]
  (let [meld&! (volatile! (transient {}))
        last-id! (volatile! (dec meld/initial-next-id))
        starts! (volatile! (sorted-map))
        offsets&! (volatile! (transient []))
        tracker (fn [reader f]
                  (let [frames-atom (.-frames reader)
                        buffer-len (.getLength (:buffer @frames-atom))]
                    (vswap! offsets&! conj! buffer-len)
                    (let [token (f)]                                                                                  ; let reader do its hard work... yielding a new token
                      (if (is-token-interesting? token)
                        (let [token-start (nth @offsets&! (dec (count @offsets&!)))
                              token-end (.getLength (:buffer @frames-atom))
                              token-text (subs source token-start token-end)                                          ; reader includes comments as part of token start-end range
                              gray-matter-end (+ token-start (measure-gray-matter token-text))                        ; we want to start after potential comments and read the meat only
                              new-node-id (vswap! last-id! inc)
                              info {:id     new-node-id
                                    :source (subs source gray-matter-end token-end)
                                    :start  gray-matter-end
                                    :end    token-end}
                              nodes-after-gray-matter (find-nodes-after-offset @starts! gray-matter-end)
                              children-ids (filter-nodes-without-parent @meld&! nodes-after-gray-matter)              ; source tracker is being called depth-first, we collect children from previous calls
                              new-node (node/make-node-from-token token info children-ids)]                           ; attach children to new node
                          (vswap! starts! assoc gray-matter-end new-node-id)
                          (vswap! meld&! assign-parent! children-ids new-node-id)
                          (vswap! meld&! assoc! new-node-id new-node)))
                      (vswap! offsets&! pop!)
                      token)))
        flush (fn []
                (meld/make (persistent! @meld&!) nil (inc @last-id!)))]
    [tracker flush]))
