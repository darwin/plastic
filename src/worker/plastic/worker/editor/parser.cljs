(ns plastic.worker.editor.parser
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.paths :as paths]
            [plastic.worker.editor.model :as editor]
            [meld.parser :as meld]))

(defn parse-source [editors [editor-selector]]
  (editor/apply-to-editors editors editor-selector
    (fn [editor]
      (let [source (editor/get-source editor)
            uri (editor/get-uri editor)
            meld (meld/parse! source uri)]
        (editor/set-meld editor meld)))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-parse-source paths/editors-path parse-source)
