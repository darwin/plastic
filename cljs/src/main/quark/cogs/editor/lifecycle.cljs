(ns quark.cogs.editor.lifecycle
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.cogs.editor.renderer :as renderer]
            [quark.schema.paths :as paths])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [dispatch react!]]))

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
        (dispatch :editor-layout editor-id)))))

(defn watch-cursors [editor-id]
  (let [cursors-subscription (subscribe [:editor-cursors editor-id])]
    (react!
      (when-let [cursors @cursors-subscription]
        (log "cursors changed" editor-id cursors)))))

(defn watch-selections [editor-id]
  (let [selections-subscription (subscribe [:editor-selections editor-id])]
    (react!
      (when-let [selections @selections-subscription]
        (log "selections in" editor-id "changed to" selections)
        (dispatch :editor-update-selections editor-id)))))

(defn watch-editing [editor-id]
  (let [editing-subscription (subscribe [:editor-editing editor-id])]
    (react!
      (when-let [_ @editing-subscription]
        (dispatch :editor-layout editor-id)))))

(defn wire-editor [editor-id]
  (watch-uri editor-id)
  (watch-raw-text editor-id)
  (watch-parse-tree editor-id)
  (watch-editing editor-id)
  (watch-cursors editor-id)
  (watch-selections editor-id))

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
  (renderer/mount-editor dom-node editor-id)
  editors)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor)
(register-handler :remove-editor paths/editors-path remove-editor)
(register-handler :mount-editor paths/editors-path mount-editor)