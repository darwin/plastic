(ns plastic.worker.undo
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]])
  (:require [plastic.undo :as undo]
            [plastic.worker.editor.model :as editor]
            [plastic.env :as env]))

; -------------------------------------------------------------------------------------------------------------------

(defn undo [context db [editor-id]]
  (let [new-db (undo/do-undo context db [editor-id])]
    (if (env/get context :log-parse-tree)
      (fancy-log "UNDO-PTREE" (editor/get-meld (get-in new-db [:editors editor-id]))))
    new-db))

(defn redo [context db [editor-id]]
  (let [new-db (undo/do-redo context db [editor-id])]
    (if (env/get context :log-parse-tree)
      (fancy-log "REDO-PTREE" (editor/get-meld (get-in new-db [:editors editor-id]))))
    new-db))

(defn push-undo [context db [editor-id description editor]]
  (let [new-db (undo/push-undo context db [editor-id description editor])]
    (if (env/get context :log-parse-tree)
      (fancy-log "PUSH-UNDO-PTREE" (editor/get-meld editor)))
    new-db))

(defn push-redo [context db [editor-id description editor]]
  (let [new-db (undo/push-redo context db [editor-id description editor])]
    (if (env/get context :log-parse-tree)
      (fancy-log "PUSH-REDO-PTREE" (editor/get-meld editor)))
    new-db))