(ns quark.cogs.editor.parser
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [rewrite-clj.parser :as parser])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]))

(defn parse-source [editors [editor-id text]]
  (let [parsed (parser/parse-string-all text)]
    (dispatch :editor-set-parsed editor-id parsed))
  editors)

(defn set-parsed [editors [editor-id parsed]]
  (assoc-in editors [editor-id :parsed] parsed))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-parse-source paths/editors-path parse-source)
(register-handler :editor-set-parsed paths/editors-path set-parsed)