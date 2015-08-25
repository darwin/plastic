(ns plastic.worker.editor.loader
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch]])
  (:require [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.paths :as paths]
            [plastic.worker.editor.model :as editor]))

(defn set-text [editors [editor-selector text]]
  (editor/apply-to-editors editors editor-selector
    (fn [editor]
      (editor/set-text editor text))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-set-text paths/editors-path set-text)
