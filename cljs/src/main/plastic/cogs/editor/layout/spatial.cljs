(ns plastic.cogs.editor.layout.spatial
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.layout.utils :as utils]))

(defn is-selectable-token? [node]
  (and (:selectable? node) (= (:tag node) :token)))

(defn add-empty-lines [selectables]
  (let [line-numbers (keys selectables)
        lines-range (range (apply min line-numbers) (apply max line-numbers))
        empty-lines (into (sorted-map) (map (fn [l] [l []]) lines-range))]
    (merge empty-lines selectables)))

(defn build-spatial-web [render-tree]
  (let [selectable-tokens-reducer (fn [accum node]
                                    (if (is-selectable-token? node)
                                      (conj accum node)
                                      accum))
        selectable-tokens (utils/reduce-render-tree selectable-tokens-reducer [] render-tree)]
    (add-empty-lines
      (into (sorted-map)
        (group-by :line
          selectable-tokens)))))                            ; guaranteed to be in left-to-right order