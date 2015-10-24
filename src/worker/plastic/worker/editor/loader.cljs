(ns plastic.worker.editor.loader
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.frame :refer [dispatch]])
  (:require [plastic.worker.editor.model :as editor]))

; -------------------------------------------------------------------------------------------------------------------

(defn set-source [context db [editor-selector source]]
  (editor/apply-to-editors context db editor-selector editor/set-source source))