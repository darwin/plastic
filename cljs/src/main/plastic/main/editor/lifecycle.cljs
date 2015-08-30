(ns plastic.main.editor.lifecycle
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [dispatch reacting! worker-dispatch]])
  (:require [plastic.util.booking :as booking]
            [plastic.util.reactions :refer [register-reaction dispose-reactions!]]
            [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.servant]
            [plastic.main.editor.render :as render]
            [plastic.main.paths :as paths]
            [plastic.onion.atom.inline-editor :as inline-editor]
            [plastic.main.editor.model :as editor]
            [plastic.undo :as undo]))

(defonce book (booking/make-booking))

; -------------------------------------------------------------------------------------------------------------------

(defn watch-to-activate-inline-editor! [editor]
  (let [editor-id (editor/get-id editor)
        editing (subscribe [:editor-editing editor-id])]
    (booking/update-item! book editor-id register-reaction
      (reacting! editing
        (fn [editing]
          (inline-editor/deactivate-inline-editor! editor-id)
          (if-not (empty? editing)
            (inline-editor/activate-inline-editor! editor-id)))))))

(defn watch-to-update-inline-editor! [editor]
  (let [editor-id (editor/get-id editor)
        inline-editor (subscribe [:editor-inline-editor editor-id])]
    (booking/update-item! book editor-id register-reaction
      (reacting! inline-editor
        (fn []
          (inline-editor/update-state! editor-id))))))

; -------------------------------------------------------------------------------------------------------------------

(defn add-editor! [editors [editor-id editor-uri]]
  (let [editor (-> (editor/make editor-id)
                 (editor/set-uri editor-uri))]
    (booking/register-item! book editor-id {})
    (worker-dispatch :add-editor editor-id editor-uri)
    (dispatch :wire-editor editor-id)
    (assoc editors editor-id editor)))

(defn remove-editor! [db [editor-id dom-node]]
  (let [editors (get db :editors)]
    (worker-dispatch :remove-editor editor-id)
    (dispose-reactions! (booking/get-item book editor-id))
    (booking/unregister-item! book editor-id)
    (render/unmount-editor dom-node)
    (-> db
      (assoc :editors (dissoc editors editor-id))
      (undo/remove-undo-redo-for-editor editor-id))))

(defn wire-editor! [editors [selector]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (watch-to-update-inline-editor! editor)
      (watch-to-activate-inline-editor! editor)
      editor)))

(defn mount-editor [editors [editor-id dom-node]]
  (render/mount-editor dom-node editor-id)
  editors)

(defn update-inline-editor [editors [selector inline-editor-state]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (let [old-inline-editor-state (editor/get-inline-editor editor)]
        (editor/set-inline-editor editor (merge old-inline-editor-state inline-editor-state))))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor!)
(register-handler :remove-editor remove-editor!)
(register-handler :mount-editor paths/editors-path mount-editor)
(register-handler :wire-editor paths/editors-path wire-editor!)
(register-handler :editor-update-inline-editor paths/editors-path update-inline-editor)
