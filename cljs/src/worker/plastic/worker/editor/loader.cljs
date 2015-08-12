(ns plastic.worker.editor.loader
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main.glue :refer [react! dispatch]])
  (:require [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.schema.paths :as paths]))

(defn set-text [editors [editor-id text]]
  (assoc-in editors [editor-id :text] text))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-set-text paths/editors-path set-text)