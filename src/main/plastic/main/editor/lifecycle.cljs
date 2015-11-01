(ns plastic.main.editor.lifecycle
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.booking :as booking]
            [plastic.frame :refer-macros [dispatch worker-dispatch]]
            [plastic.main.servant]
            [plastic.onion.inline-editor :as inline-editor]
            [plastic.main.editor.model :as editor]
            [plastic.undo :as undo]))

; -------------------------------------------------------------------------------------------------------------------

(defn watch-to-activate-inline-editor! [editor]
  (let [editor-id (editor/get-id editor)]
    (editor/subscribe! editor [:editor-editing editor-id]
      (fn [editor editing]
        (inline-editor/deactivate-inline-editor! editor)
        (if-not (empty? editing)
          (inline-editor/activate-inline-editor! editor)))))
  editor)

(defn watch-to-update-inline-editor! [editor]
  (let [editor-id (editor/get-id editor)]
    (editor/subscribe! editor [:editor-inline-editor editor-id]
      (fn [editor]
        (inline-editor/update-state! editor))))
  editor)

(defn watch-to-fetch-text! [editor]
  (let [editor-id (editor/get-id editor)]
    (editor/subscribe! editor [:editor-uri editor-id]
      (fn [editor]
        (dispatch (editor/get-context editor) [:editor-fetch-text editor-id]))))
  editor)

; -------------------------------------------------------------------------------------------------------------------

(defn add-editor! [context db [editor-id editor-uri]]
  (let [editor (-> editor-id
                 (editor/make)
                 (editor/set-context context)
                 (editor/set-uri editor-uri)
                 (editor/strip-context))
        {:keys [aux]} context]
    (booking/register-item! aux editor-id {})
    (worker-dispatch context [:add-editor editor-id editor-uri])
    (dispatch context [:wire-editor editor-id])
    (update db :editors assoc editor-id editor)))

(defn remove-editor! [context db [editor-id]]
  (let [{:keys [aux]} context]
    (worker-dispatch context [:remove-editor editor-id])
    (editor/unsubscribe-all! context editor-id)
    (booking/unregister-item! aux editor-id)
    (-> db
      (update :editors dissoc editor-id)
      (undo/remove-undo-redo-for-editor editor-id))))

(defn wire-editor! [context db [selector]]
  (editor/apply-to-editors context db selector
    (fn [editor]
      (-> editor
        (watch-to-update-inline-editor!)
        (watch-to-activate-inline-editor!)
        (watch-to-fetch-text!)))))

(defn update-inline-editor [context db [selector inline-editor-state]]
  (editor/apply-to-editors context db selector
    (fn [editor]
      (let [old-inline-editor-state (editor/get-inline-editor editor)]
        (editor/set-inline-editor editor (merge old-inline-editor-state inline-editor-state))))))