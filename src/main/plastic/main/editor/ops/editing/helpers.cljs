(ns plastic.main.editor.ops.editing.helpers
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.main.editor.model :as editor]
            [plastic.common :refer-macros [process]]
            [plastic.frame :refer [with-post-handler] :refer-macros [worker-dispatch]]
            [plastic.main.editor.ops.cursor :as cursor]))

; -------------------------------------------------------------------------------------------------------------------

(defn xform-event [editor & args]
  (vec (concat [:editor-xform (editor/get-id editor)] args)))

(defn xform-editor-on-worker [editor args & [callback]]
  (let [context (editor/get-context editor)
        post-handler (if callback
                       (fn [db]
                         (let [editor-id (editor/get-id editor)
                               editor (get-in db [:editors editor-id])
                               editor-with-context (editor/set-context editor context)
                               editor-after-callback (callback editor-with-context (editor/get-xform-report editor))
                               new-editor (editor/strip-context editor-after-callback)]
                           (if-not (identical? editor new-editor)
                             (assoc-in db [:editors editor-id] new-editor)
                             db))))]
    (worker-dispatch context (with-post-handler (apply xform-event editor args) post-handler))
    editor))

(defn continue [cb]
  (or cb identity))

(defn editing-string? [editor]
  (and (editor/editing? editor) (= (editor/get-inline-editor-mode editor) :string)))

(defn get-edit-point [editor]
  (if (editor/editing? editor)
    (editor/get-editing editor)
    (editor/get-cursor editor)))

(defn walk-structural-web [web start path]
  (process path start
    (fn [pos dir]
      (let [info (get web pos)]
        (assert info (str "broken structural web" pos " web:" (pr-str web)))
        (dir info)))))

(defn should-commit? [editor]
  (or (editor/is-inline-editor-empty? editor) (editor/is-inline-editor-modified? editor)))                            ; empty inline editor is a placeholder and must be committed regardless

(defn select-neighbour [form-id edit-point path editor]
  (let [structural-web (editor/get-structural-web-for-unit editor form-id)
        target-node-id (walk-structural-web structural-web edit-point path)]
    (editor/set-cursor editor target-node-id)))

(defn enable-editing-mode [editor]
  (-> editor
    (editor/set-editing (editor/get-cursor editor))
    (editor/set-inline-editor-puppets-state true)))                                                                   ; puppets should get enabled for each new editing session

(defn disable-editing-mode [editor]
  (-> editor
    (editor/set-editing nil)))

(defn- stuck-or-invalid-cursor? [old-cursor editor]
  (let [new-cursor (editor/get-cursor editor)]
    (or (nil? new-cursor) (= old-cursor new-cursor))))

(defn potential-cursor-move [editor]
  (let [cursor-id (editor/get-cursor editor)
        moves-to-try [:structural-left :structural-right :structural-up :spatial-up :spatial-down]
        test-editors (map (partial cursor/apply-move-cursor editor) moves-to-try)
        editors-with-valid-moves (remove (partial stuck-or-invalid-cursor? cursor-id) test-editors)]
    (if-not (empty? editors-with-valid-moves)
      (first editors-with-valid-moves))))

(defn sanitize-cursor [editor original-editor]
  (editor/move-cursor-to-valid-position-if-needed editor (iterate potential-cursor-move original-editor)))

