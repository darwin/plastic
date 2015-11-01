(ns plastic.main.editor.ops.editing
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.ops.cursor :as cursor]
            [plastic.main.editor.toolkit.id :as id]
            [plastic.main.editor.ops.editing.helpers :refer [xform-editor-on-worker continue editing-string?
                                                             get-edit-point walk-structural-web select-neighbour
                                                             should-commit? sanitize-cursor disable-editing-mode
                                                             enable-editing-mode]]))

; -------------------------------------------------------------------------------------------------------------------

(declare perform-space)

(defn start-editing [editor & [cb]]
  (if (or (editor/editing? editor) (not (editor/is-cursor-editable? editor)))
    ((continue cb) editor)
    (if (id/spot? (editor/get-cursor editor))
      (perform-space editor cb)                                                                                       ; enter edit mode by emulating a space action
      ((continue cb) (enable-editing-mode editor)))))

(defn stop-editing [editor & [cb]]
  (if-not (editor/editing? editor)
    ((continue cb) editor)
    (if-not (should-commit? editor)
      ((continue cb) (disable-editing-mode editor))
      (let [original-editor editor
            edited-node-id (editor/get-editing editor)
            value (editor/get-inline-editor-value editor)
            initial-value (editor/get-inline-editor-initial-value editor)
            effective? (editor/get-inline-editor-puppets-effective? editor)
            puppets (if effective? (editor/get-puppets editor) #{})]
        (xform-editor-on-worker (disable-editing-mode editor) [:edit edited-node-id puppets value initial-value]
          (fn [editor]
            ((continue cb) (sanitize-cursor editor original-editor))))))))

(defn apply-change-but-preserve-editing-mode [editor op & [cb]]
  (if (editor/editing? editor)
    (stop-editing editor (comp (continue cb) start-editing op))
    ((continue cb) (op editor))))

(defn perform-enter [editor & [cb]]
  (let [was-editing? (editor/editing? editor)]
    (stop-editing editor
      (fn [editor]
        (let [edit-point (get-edit-point editor)]
          (xform-editor-on-worker editor [:enter edit-point]
            (fn [editor report]
              (let [linebreak-node-id (first (:added report))]
                (assert linebreak-node-id "report didn't contain added linebreak node")
                (let [editor-with-cursor (editor/set-cursor editor linebreak-node-id)]
                  (if was-editing?
                    (perform-space editor-with-cursor cb)                                                             ; enter edit mode by emulating hitting space
                    ((continue cb) editor-with-cursor)))))))))))

(defn perform-alt-enter [editor & [cb]]
  (stop-editing editor
    (fn [editor]
      (let [edit-point (get-edit-point editor)]
        (xform-editor-on-worker editor [:alt-enter edit-point] cb)))))

(defn perform-backspace-in-empty-cell [editor & [cb]]
  {:pre [(editor/editing? editor)
         (editor/is-inline-editor-empty? editor)]}
  (apply-change-but-preserve-editing-mode editor identity cb))                                                        ; empty placeholder was removed, cursor moved left

(defn perform-backspace [editor & [cb]]
  (let [original-editor editor
        edit-point (get-edit-point editor)]
    (xform-editor-on-worker editor [:backspace edit-point]
      (fn [editor]
        ((continue cb) (sanitize-cursor editor original-editor))))))

(defn delete-after-cursor [editor & [cb]]
  (let [edit-point (get-edit-point editor)]
    (xform-editor-on-worker editor [:delete edit-point] cb)))

(defn delete-before-cursor [editor & [cb]]
  (let [edit-point (get-edit-point editor)]
    (xform-editor-on-worker editor [:alt-delete edit-point] cb)))

; -------------------------------------------------------------------------------------------------------------------

(defn open [editor op path & [cb]]
  (stop-editing editor
    (fn [editor]
      (let [edit-point (get-edit-point editor)
            focused-form-id (editor/get-focused-unit-id editor)]
        (xform-editor-on-worker editor [op edit-point]
          (fn [editor]
            (start-editing (select-neighbour focused-form-id edit-point path editor) cb)))))))

(defn perform-space [editor & [cb]]
  (open editor :space [:right] cb))

(defn open-list [editor & [cb]]
  (open editor :open-list [:right :down] cb))

(defn open-vector [editor & [cb]]
  (open editor :open-vector [:right :down] cb))

(defn open-map [editor & [cb]]
  (open editor :open-map [:right :down] cb))

(defn open-set [editor & [cb]]
  (open editor :open-set [:right :down] cb))

(defn open-fn [editor & [cb]]
  (open editor :open-fn [:right :down] cb))

(defn open-meta [editor & [cb]]
  (open editor :open-meta [:right :down] cb))

(defn open-quote [editor & [cb]]
  (open editor :open-quote [:right :down] cb))

(defn open-deref [editor & [cb]]
  (open editor :open-deref [:right :down] cb))

; -------------------------------------------------------------------------------------------------------------------

(defn next-interest [editor & [cb]]
  (apply-change-but-preserve-editing-mode editor cursor/next-interest cb))

(defn prev-interest [editor & [cb]]
  (apply-change-but-preserve-editing-mode editor cursor/prev-interest cb))

; -------------------------------------------------------------------------------------------------------------------

(defn activate-puppets [editor]
  (editor/set-inline-editor-puppets-state editor true))

(defn deactivate-puppets [editor]
  (editor/set-inline-editor-puppets-state editor false))

(defn toggle-puppets [editor]
  (if (editor/get-inline-editor-puppets-active? editor)
    (deactivate-puppets editor)
    (activate-puppets editor)))
