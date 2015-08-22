(ns plastic.worker.editor.lifecycle
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [dispatch react!]])
  (:require [plastic.util.booking :as booking]
            [plastic.util.reactions :refer [register-reaction dispose-reactions!]]
            [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.paths :as paths]
            [plastic.worker.editor.model :as editor]))

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

(defn watch-to-send-xform-report! [editor]
  (let [editor-id (editor/get-id editor)
        xform-report-subscription (subscribe [:editor-xform-report editor-id])]
    (booking/update-item! book editor-id register-reaction
      (react!
        (when-let [xform-report @xform-report-subscription]
          (dispatch :editor-set-xform-report editor-id xform-report))))))

(defn wire-editor! [editor]
  (watch-to-parse-sources! editor)
  (watch-to-update-layout! editor)
  (watch-to-send-xform-report! editor))

(defn add-editor! [editors [editor-id editor-def]]
  (let [editors (if (map? editors) editors {})
        editor (editor/make editor-id editor-def)]
    (booking/register-item! book editor-id {})
    (wire-editor! editor)
    (assoc editors editor-id editor)))

(defn remove-editor! [editors [editor-id]]
  (let [editor (get editors editor-id)]
    (dispose-reactions! editor)
    (booking/unregister-item! book editor-id)
    (dissoc editors editor-id)))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor!)
(register-handler :remove-editor paths/editors-path remove-editor!)
