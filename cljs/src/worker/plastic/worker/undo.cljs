(ns plastic.worker.undo
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]])
  (:require [plastic.worker.frame :refer [register-handler]]
            [plastic.undo :as undo]
            [plastic.worker.editor.model :as editor]))

(defn undo [db [editor-id]]
  (let [new-db (undo/do-undo db [editor-id])]
    (if plastic.env.log-parse-tree
      (fancy-log "UNDO-PTREE" (editor/get-meld (get-in new-db [:editors editor-id]))))
    new-db))

(defn redo [db [editor-id]]
  (let [new-db (undo/do-redo db [editor-id])]
    (if plastic.env.log-parse-tree
      (fancy-log "REDO-PTREE" (editor/get-meld (get-in new-db [:editors editor-id]))))
    new-db))

(defn push-undo [db [editor-id description editor]]
  (let [new-db (undo/push-undo db [editor-id description editor])]
    (if plastic.env.log-parse-tree
      (fancy-log "PUSH-UNDO-PTREE" (editor/get-meld editor)))
    new-db))

(defn push-redo [db [editor-id description editor]]
  (let [new-db (undo/push-redo db [editor-id description editor])]
    (if plastic.env.log-parse-tree
      (fancy-log "PUSH-REDO-PTREE" (editor/get-meld editor)))
    new-db))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :store-editor-undo-snapshot push-undo)
(register-handler :store-editor-redo-snapshot push-redo)

(register-handler :undo undo)
(register-handler :redo redo)
