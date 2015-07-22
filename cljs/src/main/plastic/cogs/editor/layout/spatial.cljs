(ns plastic.cogs.editor.layout.spatial
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.layout.utils :as utils]
            [plastic.util.zip :as zip-utils]
            [plastic.cogs.editor.toolkit.id :as id]))

(defn token? [selectable]
  (= (:tag selectable) :token))

(defn add-empty-lines [selectables]
  (let [line-numbers (keys selectables)
        lines-range (range (apply min line-numbers) (apply max line-numbers))
        empty-lines (into (sorted-map) (map (fn [l] [l []]) lines-range))]
    (merge empty-lines selectables)))

(defn build-spatial-web [root-loc selectables]
  (let [all-locs (zip-utils/zip-seq root-loc)
        loc-tokens (fn [loc]
                     (let [id (zip-utils/loc-id loc)
                           spot-id (id/make-spot id)]
                       [(get selectables id) (get selectables spot-id)]))
        selectable-tokens (filter token? (remove nil? (mapcat loc-tokens all-locs)))]
    (->> selectable-tokens
      (group-by :line)                                      ; guaranteed to be in left-to-right order because all-locs are left-to-right
      (into (sorted-map))
      (add-empty-lines))))