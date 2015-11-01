(ns plastic.main.editor.cursor
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.ops.editing :as editing]))

; -------------------------------------------------------------------------------------------------------------------

(defn set-cursor [context db [selector cursor]]
  (editor/apply-to-editors context db selector
    (fn [editor]
      (editing/stop-editing editor #(editor/set-cursor % cursor)))))

(defn clear-cursor [context db [selector]]
  (editor/apply-to-editors context db selector
    (fn [editor]
      (editing/stop-editing editor #(editor/set-cursor % nil)))))