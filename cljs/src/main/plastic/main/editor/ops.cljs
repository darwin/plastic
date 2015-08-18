(ns plastic.main.editor.ops
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch worker-dispatch worker-dispatch-args]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.schema.paths :as paths]
            [plastic.main.editor.ops.editing :as editing]
            [plastic.main.editor.ops.cursor :as cursor]
            [plastic.main.editor.model :as editor]))

(defn spatial-up [editor]
  (-> editor
    editing/stop-editing
    cursor/spatial-up))

(defn spatial-down [editor]
  (-> editor
    editing/stop-editing
    cursor/spatial-down))

(defn spatial-left [editor]
  (-> editor
    editing/stop-editing
    cursor/spatial-left))

(defn spatial-right [editor]
  (-> editor
    editing/stop-editing
    cursor/spatial-right))

(defn structural-left [editor]
  (-> editor
    editing/stop-editing
    cursor/structural-left))

(defn structural-right [editor]
  (-> editor
    editing/stop-editing
    cursor/structural-right))

(defn structural-up [editor]
  (-> editor
    editing/stop-editing
    cursor/structural-up))

(defn structural-down [editor]
  (let [new-editor (cursor/structural-down editor)]
    (if (identical? new-editor editor)
      (dispatch :editor-op (editor/get-id editor) :toggle-editing))
    new-editor))

(defn next-token [editor]
  (let [editor-with-moved-selection (-> editor cursor/next-token)
        moved-selection (editor/get-selection editor-with-moved-selection)]
    (-> editor
      editing/stop-editing
      (editor/set-selection moved-selection)
      editing/start-editing)))

(defn prev-token [editor]
  (let [editor-with-moved-selection (-> editor cursor/prev-token)
        moved-selection (editor/get-selection editor-with-moved-selection)]
    (-> editor
      editing/stop-editing
      (editor/set-selection moved-selection)
      editing/start-editing)))

(defn toggle-editing [editor]
  (if (editor/editing? editor)
    (editing/stop-editing editor)
    (editing/start-editing editor)))

(defn enter [editor]
  {:pre [(not (editing/editing-string? editor))]}
  (editing/perform-enter editor))

(defn alt-enter [editor]
  {:pre [(not (editing/editing-string? editor))]}
  (editing/perform-alt-enter editor))

(defn space [editor]
  {:pre [(not (editing/editing-string? editor))]}
  (editing/perform-space editor))

(defn backspace [editor]
  (if (editor/editing? editor)
    (editing/perform-backspace-in-empty-cell editor)
    (editing/perform-backspace editor)))

; ----------------------------------------------------------------------------------------------------------------------

(def ops
  {:nop              identity
   :spatial-up       spatial-up
   :spatial-down     spatial-down
   :spatial-left     spatial-left
   :spatial-right    spatial-right
   :structural-left  structural-left
   :structural-right structural-right
   :structural-up    structural-up
   :structural-down  structural-down
   :next-token       next-token
   :prev-token       prev-token
   :start-editing    editing/start-editing
   :stop-editing     editing/stop-editing
   :toggle-editing   toggle-editing
   :enter            enter
   :alt-enter        alt-enter
   :space            space
   :backspace        backspace
   :delete           editing/delete-linebreak-or-token-after-cursor
   :alt-delete       editing/delete-linebreak-or-token-before-cursor
   :open-list        editing/open-list
   :open-vector      editing/open-vector
   :open-map         editing/open-map
   :open-set         editing/open-set
   :open-fn          editing/open-fn
   :open-meta        editing/open-meta
   :open-quote       editing/open-quote
   :open-deref       editing/open-deref})

(defn dispatch-op [editors [editor-id op]]
  (let [old-editor (get editors editor-id)]
    (if-let [handler (op ops)]
      (if-let [new-editor (handler old-editor)]
        (assoc-in editors [editor-id] new-editor)
        editors)
      (do
        (error (str "Unknown editor operation '" op "'"))
        editors))))

; ----------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-op paths/editors-path dispatch-op)