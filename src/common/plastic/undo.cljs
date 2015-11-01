(ns plastic.undo
  (:require [plastic.logging :refer-macros [log info warn error group group-end fancy-log]]
            [plastic.env :as env :include-macros true]))

; -------------------------------------------------------------------------------------------------------------------

(defn remove-undo-redo-for-editor [db editor-id]
  (update db :undo-redo dissoc editor-id))

(defn debug-print-queue [q]
  (apply str (interpose "," (map #(hash (last %)) q))))

(defn debug-print-undo-redo [db editor-id]
  (let [undos (get-in db [:undo-redo editor-id :undos])
        redos (get-in db [:undo-redo editor-id :redos])]
    (str
      (count undos) " undos=[" (debug-print-queue undos) "] "
      (count redos) " redos=[" (debug-print-queue redos) "]")))

(defn- limit-queue [context q]
  (if-let [limit (env/get context :limit-undo-redo-queue)]
    (vec (take-last limit q))
    q))

; -------------------------------------------------------------------------------------------------------------------

(defn peek-queue [db editor-id key]
  (peek (get-in db [:undo-redo editor-id key])))

(defn push-queue [context db editor-id key description data]
  (let [updater (fn [items] (limit-queue context (conj (or items []) [description data])))
        res (update-in db [:undo-redo editor-id key] updater)]
    (if (env/get context :log-undo-redo)
      (fancy-log "UNDO-REDO"
        "push" editor-id key description (hash data)
        "=>" (debug-print-undo-redo res editor-id)))
    res))

(defn pop-queue [context db editor-id key]
  (when-let [undo-record (peek-queue db editor-id key)]
    (let [res (update-in db [:undo-redo editor-id key] pop)]
      (if (env/get context :log-undo-redo)
        (fancy-log "UNDO-REDO"
          "pop" editor-id key (hash (last undo-record)) undo-record
          "=>" (debug-print-undo-redo res editor-id)))
      res)))

; -------------------------------------------------------------------------------------------------------------------

(declare push-redo)

(defn peek-undo [context db editor-id]
  (peek-queue db editor-id :undos))

(defn can-undo? [context db editor-id]
  (not (nil? (peek-undo context db editor-id))))

(defn push-undo [context db [editor-id description data]]
  (push-queue context db editor-id :undos description data))

(defn pop-undo [context db editor-id]
  (pop-queue context db editor-id :undos))

(defn do-undo [context db [editor-id]]
  (if-let [undo (peek-undo context db editor-id)]
    (let [[description editor] undo
          current-editor (get-in db [:editors editor-id])
          new-db (-> db
                   ((partial pop-undo context) editor-id)
                   ((partial push-redo context) [editor-id description current-editor])
                   (assoc-in [:editors editor-id] editor))]
      new-db)
    db))

; -------------------------------------------------------------------------------------------------------------------

(defn peek-redo [context db editor-id]
  (peek-queue db editor-id :redos))

(defn can-redo? [context db editor-id]
  (not (nil? (peek-redo context db editor-id))))

(defn push-redo [context db [editor-id description data]]
  (push-queue context db editor-id :redos description data))

(defn pop-redo [context db editor-id]
  (pop-queue context db editor-id :redos))

(defn do-redo [context db [editor-id]]
  (if-let [redo (peek-redo context db editor-id)]
    (let [[description editor] redo
          current-editor (get-in db [:editors editor-id])
          new-db (-> db
                   ((partial pop-redo context) editor-id)
                   ((partial push-undo context) [editor-id description current-editor])
                   (assoc-in [:editors editor-id] editor))]
      new-db)
    db))