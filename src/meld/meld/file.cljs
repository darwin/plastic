(ns meld.file
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.node :as node]
            [meld.util :refer [update!]]
            [meld.core :as meld]))

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
        file (node/make-file source top-level-ids name)
        file-id (node/get-id file)
        meld&! (volatile! (transient meld))]
    (vswap! meld&! assoc! file-id file)
    (doseq [id top-level-ids]
      (vswap! meld&! update! id node/set-parent file-id))
    (meld/set-top-node-id (persistent! @meld&!) file-id)))
