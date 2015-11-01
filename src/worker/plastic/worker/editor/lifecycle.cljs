(ns plastic.worker.editor.lifecycle
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.worker.frame.undo :refer [set-undo-report!]]
            [plastic.worker.editor.model :as editor]
            [plastic.frame :refer-macros [dispatch]]
            [plastic.undo :as undo]
            [plastic.util.booking :as booking]))

; -------------------------------------------------------------------------------------------------------------------

(defn watch-to-parse-source! [editor]
  (let [editor-id (editor/get-id editor)]
    (editor/subscribe! editor [:editor-source editor-id]
      (fn [editor]
        (dispatch (editor/get-context editor) [:editor-parse-source editor-id]))))
  editor)

(defn watch-to-update-layout! [editor]
  (let [editor-id (editor/get-id editor)]
    (editor/subscribe! editor [:editor-meld editor-id]
      (fn [editor]
        (dispatch (editor/get-context editor) [:editor-update-layout editor-id]))))
  editor)
; -------------------------------------------------------------------------------------------------------------------

(defn add-editor! [context db [editor-id editor-uri]]
  {:pre [(not (get-in db [:editors editor-id]))]}
  (let [new-editor (editor/make editor-id editor-uri)
        {:keys [aux]} context]
    (booking/register-item! aux editor-id {})
    (dispatch context [:wire-editor editor-id])
    (update db :editors assoc editor-id new-editor)))

(defn remove-editor! [context db [editor-id]]
  {:pre [(get-in db [:editors editor-id])]}
  (let [{:keys [aux]} context]
    (editor/unsubscribe-all! context editor-id)
    (booking/unregister-item! aux editor-id)
    (-> db
      (update :editors dissoc editor-id)
      (undo/remove-undo-redo-for-editor editor-id))))

(defn wire-editor! [context db [selector]]
  (editor/apply-to-editors context db selector
    (fn [editor]
      (-> editor
        (watch-to-update-layout!)
        (watch-to-parse-source!)))))
