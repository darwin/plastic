(ns plastic.cogs.editor.lifecycle
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [dispatch react!]])
  (:require [plastic.frame :refer [subscribe register-handler]]
            [plastic.cogs.editor.render.core :as render]
            [plastic.schema.paths :as paths]
            [plastic.cogs.editor.model :as editor]))

(defn watch-uri [editor]
  (let [uri-subscription (subscribe [:editor-uri (:id editor)])]
    (editor/register-reaction editor
                              (react!
                                (when-let [uri @uri-subscription]
                                  (dispatch :editor-fetch-text (:id editor) uri))))))

(defn watch-raw-text [editor]
  (let [text-subscription (subscribe [:editor-text (:id editor)])]
    (editor/register-reaction editor
                              (react!
                                (when-let [text @text-subscription]
                                  (dispatch :editor-parse-source (:id editor) text))))))

(defn watch-parse-tree [editor]
  (let [parsed-subscription (subscribe [:editor-parse-tree (:id editor)])]
    (editor/register-reaction editor
                              (react!
                                (when-let [_ @parsed-subscription]
                                  (dispatch :editor-update-layout (:id editor)))))))

(defn watch-selections [editor]
  (let [selection-subscription (subscribe [:editor-selection (:id editor)])]
    (editor/register-reaction editor
                              (react!
                                (when-let [selections @selection-subscription]
                                  (log "selections in editor" (:id editor) "changed to" selections))))))

(defn watch-settings [editor]
  (let [code-visible-subscription (subscribe [:settings :code-visible])
        docs-visible-subscription (subscribe [:settings :docs-visible])
        headers-visible-subscription (subscribe [:settings :headers-visible])]
    (editor/register-reaction editor
                              (react!
                                (let [_ @code-visible-subscription
                                      _ @docs-visible-subscription
                                      _ @headers-visible-subscription]
                                  (dispatch :editor-update-layout (:id editor)))))))

(defn wire-editor [editor]
  (-> editor
      (watch-uri)
      (watch-raw-text)
      (watch-parse-tree)
      ;(watch-selections)                                    ; debug
      (watch-settings)))

(defn add-editor [editors [id editor-def]]
  (let [editors (if (map? editors) editors {})
        editor {:id           id
                :render-state nil
                :def          editor-def}]
    (assoc editors id (wire-editor editor))))

(defn remove-editor [editors [editor-id dom-node]]
  (let [editor (get editors editor-id)]
    (render/unmount-editor dom-node)
    (editor/dispose-reactions! editor)
    (dissoc editors editor-id)))

(defn mount-editor [editors [editor-id dom-node]]
  (render/mount-editor dom-node editor-id)
  editors)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor)
(register-handler :remove-editor paths/editors-path remove-editor)
(register-handler :mount-editor paths/editors-path mount-editor)