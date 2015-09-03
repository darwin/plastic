(ns plastic.worker.editor.layout.spatial
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.zip :as zip-utils]
            [plastic.worker.editor.toolkit.id :as id]
            [plastic.util.helpers :as helpers]
            [plastic.worker.editor.layout.utils :as utils]))

(defn add-min-max [spatial-graph]
  (let [keys (keys spatial-graph)]
    (-> spatial-graph
      (assoc :min (helpers/best-val keys <))
      (assoc :max (helpers/best-val keys >)))))

(defn build-spatial-web [root-loc selectables]
  (let [all-locs (zip-utils/zip-seq root-loc)                                                                         ; guaranteed to be in left-to-right/top-down order
        fetch-selectables (fn [loc]
                            (let [id (zip-utils/loc-id loc)
                                  spot-id (id/make-spot id)]
                              [(get selectables id) (get selectables spot-id)]))
        spatial-selectables (filter utils/spatial? (mapcat fetch-selectables all-locs))]
    (helpers/transform-map-vals (group-by :section spatial-selectables)
      (fn [section-items] (add-min-max (group-by :spatial-index section-items))))))
