(ns plastic.worker.editor.loader
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.worker.editor.model :as editor]))

; -------------------------------------------------------------------------------------------------------------------

(defn set-source [context db [editor-selector source]]
  (editor/apply-to-editors context db editor-selector editor/set-source source))