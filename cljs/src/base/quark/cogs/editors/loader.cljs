(ns quark.cogs.editors.loader
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react!]]))

(defn fetch-text [editors [editor-id uri]]
  (log "should fetch" uri)
  editors)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-fetch-text paths/editors-path fetch-text)
