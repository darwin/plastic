(ns plastic.cogs.editor.layout.spatial
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

(defn is-selectable-token? [node]
  (let [tag (:tag node)]
    (= tag :token)))

; TODO: we should use zipper and not rely on sorting property of ids at ***
(defn build-spatial-web [selectables]
  (into (sorted-map)
    (group-by :line
      (sort-by :id                                          ; *** nodes are assigned ids in left-to-right order
        (filter is-selectable-token?
          (vals selectables))))))