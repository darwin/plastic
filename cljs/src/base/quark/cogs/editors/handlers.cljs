(ns quark.cogs.editors.handlers
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [dispatch react!]]))

(defn watch-to-fetch-text [editor-id]
  (let [uri-subscription (subscribe [:editor-uri editor-id])]
    (react!
      (when-let [uri @uri-subscription]
        (dispatch :editor-fetch-text editor-id uri)))))

(defn watch-to-parse-source [editor-id]
  (let [text-subscription (subscribe [:editor-text editor-id])]
    (react!
      (when-let [text @text-subscription]
        (dispatch :editor-parse-source editor-id text)))))

(defn wire-editor [editor-id]
  (watch-to-fetch-text editor-id)
  (watch-to-parse-source editor-id))

(defn add-editor [editors [id editor-def]]
  (let [editors (if (map? editors) editors {})
        record {:render-state {:some "render state"}
                :def editor-def}]
    (wire-editor id)
    (assoc editors id record)))

(defn remove-editor [editors [editor-id]]
  (if editor-id
    (dissoc editors editor-id)
    {}))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor)
(register-handler :remove-editor paths/editors-path remove-editor)