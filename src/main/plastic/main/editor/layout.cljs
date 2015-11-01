(ns plastic.main.editor.layout
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.main.editor.model :as editor]))

; -------------------------------------------------------------------------------------------------------------------

(defn commit-layout-patch [context db [editor-selector unit-id layout-patch spatial-web-patch structural-web-patch]]
  (editor/apply-to-editors context db editor-selector
    (fn [editor]
      (-> editor
        (editor/set-layout-patch-for-unit unit-id layout-patch)
        (editor/set-spatial-web-patch-for-unit unit-id spatial-web-patch)
        (editor/set-structural-web-patch-for-unit unit-id structural-web-patch)
        (editor/set-puppets #{})
        (editor/set-highlight #{})))))

(defn remove-units [context db  [editor-selector unit-ids]]
  (editor/apply-to-editors context db editor-selector editor/remove-units unit-ids))

(defn update-units [context db  [editor-selector units]]
  (editor/apply-to-editors context db editor-selector editor/set-units units))