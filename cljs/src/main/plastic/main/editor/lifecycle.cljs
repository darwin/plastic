(ns plastic.main.editor.lifecycle
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [dispatch react! worker-dispatch]])
  (:require [plastic.util.booking :as booking]
            [plastic.util.reactions :refer [register-reaction dispose-reactions!]]
            [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.servant]
            [plastic.main.editor.render :as render]
            [plastic.main.paths :as paths]
            [plastic.main.editor.model :as editor]))

(defonce book (booking/make-booking))

(defn watch-to-fetch-text! [editor]
  (let [editor-id (editor/get-id editor)
        uri-subscription (subscribe [:editor-uri editor-id])]
    (booking/update-item! book editor-id register-reaction
      (react!
        (when-let [uri @uri-subscription]
          (dispatch :editor-fetch-text editor-id uri))))))

(defn watch-to-update-highlight! [editor]
  (let [editor-id (editor/get-id editor)
        cursor (subscribe [:editor-cursor editor-id])
        analysis (subscribe [:editor-analysis editor-id])]
    (booking/update-item! book editor-id register-reaction
      (react!
        (let [_ @cursor
              _ @analysis]
          (dispatch :editor-update-highlight editor-id))))))

(defn wire-editor! [editor]
  (watch-to-fetch-text! editor)
  (watch-to-update-highlight! editor))

(defn add-editor! [editors [editor-id editor-def]]
  (let [editors (if (map? editors) editors {})
        editor (editor/make editor-id editor-def)]
    (booking/register-item! book editor-id {})
    (wire-editor! editor)
    (worker-dispatch :add-editor editor-id editor-def)
    (assoc editors editor-id editor)))

(defn remove-editor! [editors [editor-id dom-node]]
  (render/unmount-editor dom-node)
  (dispose-reactions! (booking/get-item book editor-id))
  (booking/unregister-item! book editor-id)
  (worker-dispatch :remove-editor editor-id)
  (dissoc editors editor-id))

(defn mount-editor [editors [editor-id dom-node]]
  (render/mount-editor dom-node editor-id)
  editors)

(defn update-inline-editor [editors [selector inline-editor-state]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (editor/set-inline-editor editor inline-editor-state))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor!)
(register-handler :remove-editor paths/editors-path remove-editor!)
(register-handler :mount-editor paths/editors-path mount-editor)
(register-handler :editor-update-inline-editor paths/editors-path update-inline-editor)
