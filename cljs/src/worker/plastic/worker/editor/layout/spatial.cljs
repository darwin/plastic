(ns plastic.worker.editor.layout.spatial
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.editor.layout.utils :as utils]
            [plastic.util.zip :as zip-utils]
            [plastic.worker.editor.toolkit.id :as id]))

(defn spatial? [selectable]
  (let [tag (:tag selectable)]
    (#{:token :linebreak} tag)))

(defn build-spatial-web [root-loc selectables]
  (let [all-locs (zip-utils/zip-seq root-loc)
        fetch-selectables (fn [loc]
                            (let [id (zip-utils/loc-id loc)
                                  spot-id (id/make-spot id)]
                              [(get selectables id) (get selectables spot-id)]))
        spatial-selectables (filter spatial? (mapcat fetch-selectables all-locs))]
    (->> spatial-selectables
      (group-by :line)                                                                                                ; guaranteed to be in left-to-right order because all-locs are left-to-right
      (into (sorted-map)))))                                                                                          ; here we care about ordering by lines
