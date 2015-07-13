(ns plastic.cogs.editor.layout.spatial
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

(defn is-selectable-token? [node]
  (let [tag (:tag node)]
    (= tag :token)))

(defn add-empty-lines [selectables]
  (let [line-numbers (keys selectables)
        lines-range (range (apply min line-numbers) (apply max line-numbers))
        empty-lines (into (sorted-map) (map (fn [l] [l []]) lines-range))]
    (merge empty-lines selectables)))

; TODO: we should use zipper and not rely on sorting property of ids at ***
(defn build-spatial-web [selectables]
  (add-empty-lines
    (into (sorted-map)
      (group-by :line
        (sort-by :id                                        ; *** nodes are assigned ids in left-to-right order
          (filter is-selectable-token?
            (vals selectables)))))))