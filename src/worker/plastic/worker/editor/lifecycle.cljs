(ns plastic.worker.editor.lifecycle
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.frame.undo :refer [set-undo-report!]]
            [plastic.worker.editor.model :as editor]
            [plastic.undo :as undo]))

; -------------------------------------------------------------------------------------------------------------------

(defn add-editor! [context db [editor-id editor-uri]]
  {:pre [(not (get-in db [:editors editor-id]))]}
  (let [new-editor (editor/make editor-id editor-uri)]
    (update db :editors assoc editor-id new-editor)))

(defn remove-editor! [context db [editor-id]]
  {:pre [(get-in db [:editors editor-id])]}
  (-> db
    (update :editors dissoc editor-id)
    (undo/remove-undo-redo-for-editor editor-id)))