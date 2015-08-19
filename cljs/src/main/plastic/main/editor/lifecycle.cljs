(ns plastic.main.editor.lifecycle
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [dispatch react! worker-dispatch]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.servant]
            [plastic.main.editor.render :as render]
            [plastic.main.paths :as paths]
            [plastic.main.editor.model :as editor]))

(defn watch-uri [editor]
  (let [editor-id (editor/get-id editor)
        uri-subscription (subscribe [:editor-uri editor-id])]
    (editor/register-reaction editor
      (react!
        (when-let [uri @uri-subscription]
          (dispatch :editor-fetch-text editor-id uri))))))

(defn watch-for-highlight-updates [editor]
  (let [editor-id (editor/get-id editor)
        cursor (subscribe [:editor-cursor editor-id])
        analysis (subscribe [:editor-analysis editor-id])]
    (editor/register-reaction editor
      (react!
        (let [_ @cursor
              _ @analysis]
          (dispatch :editor-update-highlight editor-id))))))

(defn wire-editor [editor]
  (-> editor
    (watch-uri)
    (watch-for-highlight-updates)))

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

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor)
(register-handler :remove-editor paths/editors-path remove-editor)
(register-handler :mount-editor paths/editors-path mount-editor)
