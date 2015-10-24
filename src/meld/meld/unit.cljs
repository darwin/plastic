(ns meld.unit
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [meld.zip :as zip]
            [meld.node :as node]
            [meld.util :refer [update! transplant-meta]]
            [meld.core :as meld]))

; -------------------------------------------------------------------------------------------------------------------
; unit is a generalization of top-level-form
; we are comment/whitespace aware, so not only code forms can appear at the top level
; independent linebreaks and comments are treated as top level forms too
;
; so, in unit we want to capture top-level forms, linebreaks and comments
; in case of form we should include following comment and linebreak
; in case of comment we should include following linebreak
; in all cases we want following whitespace to be included as well

(defn make-unit-detector []
  (let [prev-tag! (volatile! :linebreak)
        group! (volatile! 0)]
    (fn [loc]
      (let [tag (zip/get-tag loc)]
        (case tag
          (:comment :linebreak :whitespace) (if (#{:linebreak} @prev-tag!) (vswap! group! inc))
          (vswap! group! inc))
        (vreset! prev-tag! tag))
      @group!)))

(defn merge-nodes-info [meld& node-ids]
  (loop [ids node-ids
         start nil
         end nil
         source ""]
    (if (empty? ids)
      [start end source]
      (let [node (meld/get-node meld& (first ids))
            node-source (node/get-source node)
            [node-start node-end] (node/get-range node)
            new-start (if start (min start node-start) node-start)
            new-end (if end (max end node-end) node-end)
            new-source (str source node-source)]
        (recur (rest ids) new-start new-end new-source)))))

(defn reparent [meld& ids new-parent]
  (let [* (fn [meld& id] (update! meld& id node/set-parent new-parent))]
    (reduce * meld& ids)))

(defn group-into-units [meld]
  (let [file-loc (zip/zip meld)
        file-id (zip/get-id file-loc)
        first-loc (zip/down file-loc)
        partitions (partition-by (make-unit-detector) (take-while zip/good? (iterate zip/right first-loc)))
        unit-children-ids (map #(map zip/get-id %) partitions)
        unit-ids! (volatile! [])
        next-id! (volatile! (meld/get-next-node-id meld))
        * (fn [meld& ids]
            (let [[start end source] (merge-nodes-info meld& ids)
                  unit-id @next-id!
                  unit-node (-> (node/make-unit source ids start end)
                              (node/set-id unit-id)
                              (node/set-parent file-id))]
              (vswap! next-id! inc)
              (vswap! unit-ids! conj unit-id)
              (-> meld&
                (assoc! unit-id unit-node)
                (reparent ids unit-id))))
        meld& (reduce * (transient meld) unit-children-ids)
        new-meld& (update! meld& file-id node/set-children @unit-ids!)]
    (-> (transplant-meta (persistent! new-meld&) meld)
      (meld/set-next-node-id @next-id!))))

