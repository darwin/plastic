(ns plastic.main.editor.selection
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.ops.editing :as editing]))

; -------------------------------------------------------------------------------------------------------------------

(defn clear-selection [context db [selector]]
  (editor/apply-to-editors context db selector
    (fn [editor]
      (editing/stop-editing editor #(editor/set-selection % #{})))))

(defn set-selection [context db  [selector selection]]
  (editor/apply-to-editors context db selector
    (fn [editor]
      (editing/stop-editing editor #(editor/set-selection % selection)))))

(defn toggle-selection [context db  [selector selection]]
  (editor/apply-to-editors context db selector
    (fn [editor]
      (editing/stop-editing editor #(editor/toggle-selection % selection)))))