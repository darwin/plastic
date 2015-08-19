(ns plastic.main.editor.lifecycle
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [dispatch react! worker-dispatch]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.servant]
            [plastic.main.editor.render :as render]
            [plastic.main.paths :as paths]
            [plastic.main.editor.model :as editor]))

(defn watch-uri [editor]
  (let [uri-subscription (subscribe [:editor-uri (:id editor)])]
    (editor/register-reaction editor
      (react!
        (when-let [uri @uri-subscription]
          (dispatch :editor-fetch-text (:id editor) uri))))))

(defn watch-settings [editor]
  (let [code-visible-subscription (subscribe [:settings :code-visible])
        docs-visible-subscription (subscribe [:settings :docs-visible])
        headers-visible-subscription (subscribe [:settings :headers-visible])]
    (editor/register-reaction editor
      (react!
        (let [_ @code-visible-subscription
              _ @docs-visible-subscription
              _ @headers-visible-subscription]
          (worker-dispatch :editor-update-layout (:id editor)))))))

(defn wire-editor [editor]
  editor
  (-> editor
    (watch-uri)
    (watch-settings)))

(defn add-editor [editors [editor-id editor-def]]
  (let [editors (if (map? editors) editors {})
        editor {:id  editor-id
                :def editor-def}]
    (worker-dispatch :add-editor editor-id editor-def)
    (assoc editors editor-id (wire-editor editor))))

(defn remove-editor [editors [editor-id dom-node]]
  (let [editor (get editors editor-id)]
    (render/unmount-editor dom-node)
    (editor/dispose-reactions! editor)
    (worker-dispatch :remove-editor editor-id)
    (dissoc editors editor-id)))

(defn mount-editor [editors [editor-id dom-node]]
  (render/mount-editor dom-node editor-id)
  editors)

; ----------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor)
(register-handler :remove-editor paths/editors-path remove-editor)
(register-handler :mount-editor paths/editors-path mount-editor)
