(ns plastic.worker.editor.layout.spatial
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.editor.toolkit.id :as id]
            [plastic.util.helpers :as helpers]
            [plastic.worker.editor.layout.utils :as utils]
            [meld.zip :as zip]))

(defn add-min-max [spatial-graph]
  (let [keys (keys spatial-graph)]
    (-> spatial-graph
      (assoc :min (helpers/best-val keys <))
      (assoc :max (helpers/best-val keys >)))))

(defn build-spatial-web [root-loc selectables]
  (let [ids (zip/descendants root-loc)                                                                           ; guaranteed to be in left-to-right/top-down order
        fetch-selectables (fn [id]
                            (let [spot-id (id/make-spot id)]
                              [(get selectables id) (get selectables spot-id)]))
        spatial-selectables (filter utils/spatial? (mapcat fetch-selectables ids))]
    (helpers/process-map (group-by :section spatial-selectables)
      (fn [section-items]
        (add-min-max (group-by :spatial-index section-items))))))
