(ns plastic.worker.editor.lifecycle
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [dispatch main-dispatch react!]])
  (:require [plastic.util.reactions :refer [register-reaction dispose-reactions!]]
            [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.frame.undo :refer [set-undo-report!]]
            [plastic.worker.paths :as paths]
            [plastic.worker.editor.watcher :as watcher]
            [plastic.worker.editor.model :as editor]
            [plastic.undo :as undo]))

(defn add-editor! [editors [editor-id editor-uri]]
  (let [editors (if (map? editors) editors {})
        editor (editor/make editor-id editor-uri)]
    (watcher/init-editor editor-id)
    (assoc editors editor-id editor)))

(defn remove-editor! [db [editor-id]]
  (let [editors (get db :editors)]
    (-> db
      (assoc :editors (dissoc editors editor-id))
      (undo/remove-undo-redo-for-editor editor-id))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor!)
(register-handler :remove-editor remove-editor!)
