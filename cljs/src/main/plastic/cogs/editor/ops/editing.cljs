(ns plastic.cogs.editor.ops.editing
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.render.dom :as dom]
            [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.cogs.editor.model :as editor]
            [plastic.cogs.editor.ops.selection :as selection]
            [plastic.onion.core :as onion]
            [plastic.util.zip :as zip-utils]
            [rewrite-clj.zip :as zip]))

(defn select-next-candidate-for-case-of-selected-node-removal [editor]
  (selection/apply-move-selection editor :spatial-left :spatial-right :structural-up))

(defn get-selected-node-id [editor]
  (first (editor/get-selection editor)))

(defn get-edited-node-id [editor]
  (first (editor/get-editing-set editor)))

(defn set-selection-to-node-if-exists [editor node-id]
  (let [node-loc (editor/find-node-loc editor node-id)]
    (if (zip-utils/valid-loc? node-loc)
      (editor/set-selection editor #{node-id})
      editor)))

(defn commit-value [editor value]
  (let [node-id (get-edited-node-id editor)]
    (-> editor
      (select-next-candidate-for-case-of-selected-node-removal)
      (editor/commit-node-value node-id value)
      (editor/update-render-tree-node-in-focused-form node-id value) ; ugly: this prevents brief display of previous value before re-layouting finishes
      (set-selection-to-node-if-exists node-id))))

(defn apply-editing [editor action]
  (let [should-be-editing? (= action :start)
        can-edit? (editor/can-edit-selection? editor)]
    (editor/set-editing-set editor (if (and can-edit? should-be-editing?) (editor/get-selection editor)))))

(defn should-commit? [editor-id]
  (and (onion/is-inline-editor-modified? editor-id)))

; ----------------------------------------------------------------------------------------------------------------

(defn start-editing [editor]
  (apply-editing editor :start))

(defn stop-editing [editor]
  (or
    (if (editor/editing? editor)
      (let [editor-id (editor/get-id editor)
            editor-after-commit (if (should-commit? editor-id)
                                  (commit-value editor (onion/get-postprocessed-value-after-editing editor-id))
                                  editor)]
        (apply-editing editor-after-commit :stop)))
    editor))

(defn insert-and-start-editing [editor selected-node-id & values]
  (let [node-id (if (editor/editing? editor) (get-edited-node-id editor) (get-selected-node-id editor))
        editor-after-stop-editing (if (editor/editing? editor) (stop-editing editor) editor)]
    (-> editor-after-stop-editing
      (editor/insert-values-after-node node-id values)
      (set-selection-to-node-if-exists selected-node-id)
      (start-editing))))

(defn prepend-and-keep-selection [editor & values]
  (let [selected-id (get-selected-node-id editor)]
    (-> editor
      (editor/insert-values-before-node selected-id values)
      (set-selection-to-node-if-exists selected-id))))

(defn dispatch-atom-command-in-inline-editor [editor command]
  (let [editor-id (editor/get-id editor)]
    (onion/dispatch-command-in-inline-editor editor-id command)
    editor))

(defn insert-text-into-inline-editor [editor text]
  (let [editor-id (editor/get-id editor)]
    (onion/insert-text-into-inline-editor editor-id text)
    editor))

(defn editing-string-or-doc? [editor]
  (if (editor/editing? editor)
    (let [editor-id (editor/get-id editor)
          mode (onion/get-inline-editor-mode editor-id)]
      (or (= mode :string) (= mode :doc)))))

(defn enter [editor]
  (if (editing-string-or-doc? editor)
    (dispatch-atom-command-in-inline-editor editor "editor:newline")
    (if (editor/editing? editor)
      (let [placeholder-node (editor/prepare-placeholder-node)]
        (insert-and-start-editing editor (:id placeholder-node) (editor/prepare-newline-node) placeholder-node))
      (prepend-and-keep-selection editor (editor/prepare-newline-node)))))

(defn space [editor]
  (if (editing-string-or-doc? editor)
    (insert-text-into-inline-editor editor " ")
    (let [placeholder-node (editor/prepare-placeholder-node)]
      (insert-and-start-editing editor (:id placeholder-node) placeholder-node))))

(defn delete-selection [editor]
  {:pre [(not (editor/editing? editor))]}
  (let [node-id (get-selected-node-id editor)
        editor-with-next-selection (select-next-candidate-for-case-of-selected-node-removal editor)
        next-selection-node-id (get-selected-node-id editor-with-next-selection)]
    (-> editor
      (editor/delete-node node-id)
      (set-selection-to-node-if-exists next-selection-node-id))))

(defn delete-and-move-left [editor]
  (let [node-loc (editor/find-node-loc editor (get-edited-node-id editor))
        left-loc (zip/left node-loc)]
    (if (zip-utils/valid-loc? left-loc)
      (-> editor
        (stop-editing)
        (set-selection-to-node-if-exists (zip-utils/loc-id left-loc)))
      (-> editor
        (stop-editing)
        (selection/structural-up)))))

(defn delete-char-or-move [editor]
  {:pre [(editor/editing? editor)]}
  (let [editor-id (editor/get-id editor)]
    (if (onion/is-inline-editor-empty? editor-id)
      (delete-and-move-left editor)
      (do
        (onion/dispatch-command-in-inline-editor editor-id "core:backspace")
        editor))))