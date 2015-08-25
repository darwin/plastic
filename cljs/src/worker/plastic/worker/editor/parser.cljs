(ns plastic.worker.editor.parser
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [react! dispatch]])
  (:require [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.paths :as paths]
            [plastic.worker.editor.parser.utils :as utils]
            [rewrite-clj.parser :as rewrite-cljs]
            [plastic.worker.editor.model :as editor]))

(defn parse-source [editors [editor-selector]]
  (editor/apply-to-editors editors editor-selector
    (fn [editor]
      (let [text (editor/get-text editor)
            parse-tree (rewrite-cljs/parse-string-all text)
            unique-parse-tree (utils/make-nodes-unique parse-tree)]
        (editor/set-parse-tree editor unique-parse-tree)))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-parse-source paths/editors-path parse-source)
