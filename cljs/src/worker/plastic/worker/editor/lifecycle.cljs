(ns plastic.worker.editor.lifecycle
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [dispatch react!]])
  (:require [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.schema.paths :as paths]
            [plastic.worker.editor.model :as editor]))

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

(defn wire-editor [editor]
  (-> editor
    (watch-raw-text)
    (watch-parse-tree)))

(defn add-editor [editors [id editor-def]]
  (let [editors (if (map? editors) editors {})
        editor {:id  id
                :def editor-def}]
    (assoc editors id (wire-editor editor))))

(defn remove-editor [editors [editor-id]]
  (let [editor (get editors editor-id)]
    (editor/dispose-reactions! editor)
    (dissoc editors editor-id)))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor)
(register-handler :remove-editor paths/editors-path remove-editor)