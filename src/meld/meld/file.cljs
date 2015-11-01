(ns meld.file
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [meld.node :as node]
            [meld.util :refer [update! transplant-meta]]
            [meld.core :as meld]))

; -------------------------------------------------------------------------------------------------------------------

(defn find-top-level-ids-in-order [meld]
  (let [by-start (fn [[_ a] [_ b]]
                   (let [start-a (node/get-start a)
                         start-b (node/get-start b)]
                     (compare start-a start-b)))
        has-parent? (fn [[_id node]]
                      (node/get-parent node))]
    (map first (sort by-start (remove has-parent? meld)))))

(defn wrap-all-as-file [meld source name]
  (let [top-level-ids (find-top-level-ids-in-order meld)
        file-id (meld/get-next-node-id meld)
        file-node (node/set-id (node/make-file source top-level-ids name) file-id)
        meld&! (volatile! (transient meld))]
    (vswap! meld&! assoc! file-id file-node)
    (doseq [id top-level-ids]
      (vswap! meld&! update! id node/set-parent file-id))
    (-> (transplant-meta (persistent! @meld&!) meld)
      (meld/set-root-node-id file-id)
      (meld/set-next-node-id (inc file-id)))))
