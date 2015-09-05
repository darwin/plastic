(ns plastic.main.editor.loader
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch worker-dispatch]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.paths :as paths]
            [plastic.onion.atom :as atom]
            [plastic.main.editor.model :as editor]))

(defn fetch-text [editors [editor-selector]]
  (editor/apply-to-editors editors editor-selector
    (fn [editor]
      (let [editor-id (editor/get-id editor)
            uri (editor/get-uri editor)]
        (atom/load-file-content uri #(worker-dispatch :editor-set-source editor-id %))
        editor))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-fetch-text paths/editors-path fetch-text)
