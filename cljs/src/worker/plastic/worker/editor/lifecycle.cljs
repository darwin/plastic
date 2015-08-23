(ns plastic.worker.editor.lifecycle
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [dispatch main-dispatch react!]])
  (:require [plastic.util.booking :as booking]
            [plastic.util.reactions :refer [register-reaction dispose-reactions!]]
            [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.frame.undo :refer [set-undo-report!]]
            [plastic.worker.paths :as paths]
            [plastic.worker.editor.model :as editor]
            [plastic.undo :as undo]))

(defonce book (booking/make-booking))

(defn watch-to-parse-sources! [editor]
  (let [editor-id (editor/get-id editor)
        text-subscription (subscribe [:editor-text editor-id])]
    (booking/update-item! book editor-id register-reaction
      (react!
        (when-let [text @text-subscription]
          (dispatch :editor-parse-source editor-id text))))))

(defn watch-to-update-layout! [editor]
  (let [editor-id (editor/get-id editor)
        parsed-subscription (subscribe [:editor-parse-tree editor-id])]
    (booking/update-item! book editor-id register-reaction
      (react!
        (when-let [_ @parsed-subscription]
          (dispatch :editor-update-layout editor-id))))))

(defn wire-editor! [editor]
  (watch-to-parse-sources! editor)
  (watch-to-update-layout! editor))

(defn add-editor! [editors [editor-id editor-def]]
  (let [editors (if (map? editors) editors {})
        editor (editor/make editor-id editor-def)]
    (booking/register-item! book editor-id {})
    (wire-editor! editor)
    (assoc editors editor-id editor)))

(defn remove-editor! [db [editor-id]]
  (let [editors (get db :editors)]
    (dispose-reactions! (booking/get-item book editor-id))
    (booking/unregister-item! book editor-id)
    (-> db
      (assoc :editors (dissoc editors editor-id))
      (undo/remove-undo-redo-for-editor editor-id))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor!)
(register-handler :remove-editor remove-editor!)
