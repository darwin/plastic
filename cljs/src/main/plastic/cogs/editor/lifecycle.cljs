(ns plastic.cogs.editor.lifecycle
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [dispatch react!]])
  (:require [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.cogs.editor.render.core :as render]
            [plastic.schema.paths :as paths]))

(defn watch-uri [editor-id]
  (let [uri-subscription (subscribe [:editor-uri editor-id])]
    (react!
      (when-let [uri @uri-subscription]
        (dispatch :editor-fetch-text editor-id uri)))))

(defn watch-raw-text [editor-id]
  (let [text-subscription (subscribe [:editor-text editor-id])]
    (react!
      (when-let [text @text-subscription]
        (dispatch :editor-parse-source editor-id text)))))

(defn watch-parse-tree [editor-id]
  (let [parsed-subscription (subscribe [:editor-parse-tree editor-id])]
    (react!
      (when-let [_ @parsed-subscription]
        (dispatch :editor-update-layout editor-id)
        (dispatch :editor-run-analysis editor-id)))))

(defn watch-selections [editor-id]
  (let [selection-subscription (subscribe [:editor-selection editor-id])]
    (react!
      (when-let [selections @selection-subscription]
        (log "selections in editor" editor-id "changed to" selections)))))

(defn watch-settings [editor-id]
  (let [code-visible-subscription (subscribe [:settings :code-visible])
        docs-visible-subscription (subscribe [:settings :docs-visible])
        headers-visible-subscription (subscribe [:settings :headers-visible])]
    (react!
      (let [_ @code-visible-subscription
            _ @docs-visible-subscription
            _ @headers-visible-subscription]
        (dispatch :editor-update-layout editor-id)))))

(defn wire-editor [editor-id]
  (watch-uri editor-id)
  (watch-raw-text editor-id)
  (watch-parse-tree editor-id)
  (watch-selections editor-id)                              ; debug
  (watch-settings editor-id))

(defn add-editor [editors [id editor-def]]
  (let [editors (if (map? editors) editors {})
        record {:id           id
                :render-state nil
                :def          editor-def}]
    (wire-editor id)
    (assoc editors id record)))

(defn remove-editor [editors [editor-id]]
  (if editor-id
    (dissoc editors editor-id)
    {}))

(defn mount-editor [editors [editor-id dom-node]]
  (render/mount-editor dom-node editor-id)
  editors)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor)
(register-handler :remove-editor paths/editors-path remove-editor)
(register-handler :mount-editor paths/editors-path mount-editor)