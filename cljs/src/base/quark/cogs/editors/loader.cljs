(ns quark.cogs.editors.loader
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [quark.onion.core :as onion])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]))

(defn fetch-text [editors [editor-id uri]]
  (onion/load-file-content uri #(dispatch :editor-set-text editor-id %))
  editors)

(defn set-text [editors [editor-id text]]
  (assoc-in editors [editor-id :text] text))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-fetch-text paths/editors-path fetch-text)
(register-handler :editor-set-text paths/editors-path set-text)