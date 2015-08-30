(ns plastic.main.editor.ops.editing
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [worker-dispatch worker-dispatch-with-effect worker-dispatch-args]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.ops.cursor :as cursor]
            [plastic.main.editor.toolkit.id :as id]))

(defn xform-event [editor & args]
  (vec (concat [:editor-xform (editor/get-id editor)] args)))

(defn xform-editor-on-worker [editor args & [callback]]
  (worker-dispatch-args (apply xform-event editor args)
    (if callback (editor/db-updater (editor/get-id editor) callback)))
  editor)

(defn continue [cb]
  (if cb cb identity))

(defn move-cursor-for-case-of-selected-node-removal [editor]
  (let [cursor-id (id/id-part (editor/get-cursor editor))
        moves-to-try (if (= cursor-id (editor/get-focused-form-id editor))
                       [:move-next-form :move-prev-form]                                                              ; a case of deleting whole focused form
                       [:structural-left :structural-right :structural-up])]
    (apply cursor/apply-move-cursor editor moves-to-try)))

(defn editing-string? [editor]
  (and (editor/editing? editor) (= (editor/get-inline-editor-mode editor) :string)))

(defn get-edit-point [editor]
  (if (editor/editing? editor)
    (editor/get-editing editor)
    (editor/get-cursor editor)))

(defn walk-structural-web [web start path]
  (let [walker (fn [pos dir]
                 (let [info (get web pos)
                       _ (assert info)
                       new-pos (dir info)]
                   new-pos))]
    (reduce walker start path)))

(defn should-commit? [editor]
  (or (editor/is-inline-editor-empty? editor) (editor/is-inline-editor-modified? editor)))                            ; empty inline editor is a placeholder and must be comitted regardless

(defn select-neighbour [form-id edit-point path editor]
  (let [structural-web (editor/get-structural-web-for-form editor form-id)
        target-node-id (walk-structural-web structural-web edit-point path)]
    (editor/set-cursor editor target-node-id)))

(defn enable-editing-mode [editor]
  (-> editor
    (editor/set-editing (editor/get-cursor editor))
    (editor/set-inline-editor-puppets-state true)))                                                                   ; puppets should get enabled for each new editing session

(defn disable-editing-mode [editor]
  (-> editor
    (editor/set-editing nil)))

(defn sanitize-cursor [editor alt-cursor]
  (-> editor
    (editor/replace-cursor-if-not-valid alt-cursor)))

; -------------------------------------------------------------------------------------------------------------------

(declare perform-space)

(defn start-editing [editor & [cb]]
  (if (editor/editing? editor)
    ((continue cb) editor)
    (if (id/spot? (editor/get-cursor editor))
      (perform-space editor cb)                                                                                       ; enter edit mode by emulating hitting space
      ((continue cb) (enable-editing-mode editor)))))

(defn stop-editing [editor & [cb]]
  (if-not (editor/editing? editor)
    ((continue cb) editor)
    (if-not (should-commit? editor)
      ((continue cb) (disable-editing-mode editor))
      (let [edited-node-id (editor/get-editing editor)
            value (editor/get-inline-editor-value editor)
            moved-cursor (editor/get-cursor (move-cursor-for-case-of-selected-node-removal editor))
            effective? (editor/get-inline-editor-puppets-effective? editor)
            puppets (if effective? (editor/get-puppets editor) #{})]
        (xform-editor-on-worker (disable-editing-mode editor) [:edit edited-node-id puppets value]
          (fn [editor]
            ((continue cb) (sanitize-cursor editor moved-cursor))))))))

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
  (let [edit-point (get-edit-point editor)
        editor-with-moved-cursor (move-cursor-for-case-of-selected-node-removal editor)
        moved-cursor (editor/get-cursor editor-with-moved-cursor)]
    (xform-editor-on-worker editor [:backspace edit-point]
      (fn [editor]
        ((continue cb) (sanitize-cursor editor moved-cursor))))))

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
            focused-form-id (editor/get-focused-form-id editor)]
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

(defn next-token [editor & [cb]]
  (apply-change-but-preserve-editing-mode editor cursor/next-token cb))

(defn prev-token [editor & [cb]]
  (apply-change-but-preserve-editing-mode editor cursor/prev-token cb))

; -------------------------------------------------------------------------------------------------------------------

(defn activate-puppets [editor]
  (editor/set-inline-editor-puppets-state editor true))

(defn deactivate-puppets [editor]
  (editor/set-inline-editor-puppets-state editor false))

(defn toggle-puppets [editor]
  (if (editor/get-inline-editor-puppets-active? editor)
    (deactivate-puppets editor)
    (activate-puppets editor)))
