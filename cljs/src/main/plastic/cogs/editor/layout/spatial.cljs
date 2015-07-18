(ns plastic.cogs.editor.layout.spatial
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.layout.utils :as utils]
            [plastic.util.zip :as zip-utils]))

(defn token? [selectable]
  (= (:tag selectable) :token))

(defn add-empty-lines [selectables]
  (let [line-numbers (keys selectables)
        lines-range (range (apply min line-numbers) (apply max line-numbers))
        empty-lines (into (sorted-map) (map (fn [l] [l []]) lines-range))]
    (merge empty-lines selectables)))

(defn build-spatial-web [root-loc selectables]
  (let [all-locs (zip-utils/zip-seq root-loc)
        selectable-tokens (filter token? (keep #(get selectables (zip-utils/loc-id %)) all-locs))]
    (add-empty-lines
      (into (sorted-map)
        (group-by :line
          selectable-tokens)))))                            ; guaranteed to be in left-to-right order because all-locs are left-to-right