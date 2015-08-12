(ns plastic.main.editor.loader
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main.glue :refer [react! dispatch worker-dispatch]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.schema.paths :as paths]
            [plastic.onion.core :as onion]))

(defn fetch-text [editors [editor-id uri]]
  (onion/load-file-content uri #(worker-dispatch :editor-set-text editor-id %))
  editors)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-fetch-text paths/editors-path fetch-text)