(ns plastic.worker.editor.parser
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker.glue :refer [react! dispatch]])
  (:require [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.schema.paths :as paths]
            [plastic.worker.editor.parser.utils :as utils]
            [rewrite-clj.parser :as rewrite-cljs]))

(defn parse-source [editors [editor-id text]]
  (let [parse-tree (rewrite-cljs/parse-string-all text)
        unique-parse-tree (utils/make-nodes-unique parse-tree)]
    (dispatch :editor-set-parse-tree editor-id unique-parse-tree))
  editors)

(defn set-parse-tree [editors [editor-id parse-tree]]
  (assoc-in editors [editor-id :parse-tree] parse-tree))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-parse-source paths/editors-path parse-source)
(register-handler :editor-set-parse-tree paths/editors-path set-parse-tree)