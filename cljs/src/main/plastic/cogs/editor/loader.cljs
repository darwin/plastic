(ns plastic.cogs.editor.loader
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]])
  (:require [plastic.frame :refer [subscribe register-handler]]
            [plastic.schema.paths :as paths]
            [plastic.onion.core :as onion]))

(defn fetch-text [editors [editor-id uri]]
  (onion/load-file-content uri #(dispatch :editor-set-text editor-id %))
  editors)

(defn set-text [editors [editor-id text]]
  (assoc-in editors [editor-id :text] text))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-fetch-text paths/editors-path fetch-text)
(register-handler :editor-set-text paths/editors-path set-text)