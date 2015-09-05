(ns plastic.worker.editor.loader
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch]])
  (:require [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.paths :as paths]
            [plastic.worker.editor.model :as editor]))

(defn set-source [editors [editor-selector source]]
  (editor/apply-to-editors editors editor-selector
    (fn [editor]
      (-> editor
        (editor/set-source source)))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-set-source paths/editors-path set-source)
