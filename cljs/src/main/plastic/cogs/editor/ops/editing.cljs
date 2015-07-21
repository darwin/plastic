(ns plastic.cogs.editor.ops.editing
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.render.dom :as dom]
            [plastic.frame :refer [subscribe register-handler]]
            [plastic.cogs.editor.model :as editor]
            [plastic.cogs.editor.ops.cursor :as selection]
            [plastic.onion.core :as onion]
            [plastic.util.zip :as zip-utils]
            [rewrite-clj.zip :as zip]
            [plastic.cogs.editor.toolkit.id :as id]))

(defn select-next-thing-for-case-of-selected-node-removal [editor]
  (let [cursor-id (id/id-part (editor/get-cursor editor))
        moves-to-try (if (= cursor-id (editor/get-focused-form-id editor))
                       [:move-next-form :move-prev-form]    ; case of deleting whole focused form
                       [:structural-left :structural-right :structural-up])]
    (apply selection/apply-move-cursor editor moves-to-try)))

(defn set-cursor-to-node-if-exists [editor node-id]
  (let [node-loc (editor/find-node-loc editor node-id)]
    (if (zip-utils/valid-loc? node-loc)
      (editor/set-cursor editor node-id)
      (editor/clear-cursor-if-invalid editor true))))

(defn commit-value [editor value]
  (let [node-id (editor/get-editing editor)]
    (-> editor
      (select-next-thing-for-case-of-selected-node-removal)
      (editor/commit-node-value node-id value)
      (editor/update-layout-node-in-focused-form node-id value) ; ugly: this prevents brief display of previous value before re-layouting finishes
      (set-cursor-to-node-if-exists node-id))))

(defn apply-editing [editor action]
  (let [should-be-editing? (= action :start)
        can-edit? (editor/can-edit-cursor? editor)]
    (editor/set-editing editor (if (and can-edit? should-be-editing?) (editor/get-cursor editor)))))

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

(defn insert-and-start-editing [editor node-id-to-be-edited & values]
  (let [current-node-id (if (editor/editing? editor) (editor/get-editing editor) (editor/get-cursor editor))]
    (-> editor
      (editor/insert-values-after-node current-node-id values)
      (stop-editing)
      (set-cursor-to-node-if-exists node-id-to-be-edited)
      (start-editing))))

(defn prepend-and-keep-selection [editor & values]
  (let [cursor-id (editor/get-cursor editor)]
    (-> editor
      (editor/insert-values-before-node cursor-id values)
      (set-cursor-to-node-if-exists cursor-id))))

(defn append-and-keep-selection [editor & values]
  (let [cursor-id (editor/get-cursor editor)]
    (-> editor
      (editor/insert-values-after-node cursor-id values)
      (set-cursor-to-node-if-exists cursor-id))))

(defn dispatch-atom-command-in-inline-editor [editor command]
  (let [editor-id (editor/get-id editor)]
    (onion/dispatch-command-in-inline-editor editor-id command)
    editor))

(defn insert-text-into-inline-editor [editor text]
  (let [editor-id (editor/get-id editor)]
    (onion/insert-text-into-inline-editor editor-id text)
    editor))

(defn editing-string? [editor]
  (if (editor/editing? editor)
    (let [editor-id (editor/get-id editor)
          mode (onion/get-inline-editor-mode editor-id)]
      (= mode :string))))

(defn enter [editor]
  (if (editing-string? editor)
    (dispatch-atom-command-in-inline-editor editor "editor:newline")
    (let [placeholder-node (editor/prepare-placeholder-node)]
      (insert-and-start-editing editor (:id placeholder-node) (editor/prepare-newline-node) placeholder-node))))

(defn alt-enter [editor]
  (if (editing-string? editor)
    editor
    (editor/remove-linebreak-before-node editor (editor/get-cursor editor))))

(defn space [editor]
  (if (editing-string? editor)
    (insert-text-into-inline-editor editor " ")
    (let [placeholder-node (editor/prepare-placeholder-node)]
      (insert-and-start-editing editor (:id placeholder-node) placeholder-node))))

(defn delete-selection [editor]
  {:pre [(not (editor/editing? editor))]}
  (if-let [cursor-id (editor/get-cursor editor)]
    (let [editor-with-next-selection-and-focus (select-next-thing-for-case-of-selected-node-removal editor)
          next-cursor-id (editor/get-cursor editor-with-next-selection-and-focus)]
      (-> editor
        (editor/delete-node cursor-id)
        (set-cursor-to-node-if-exists next-cursor-id)))
    editor))

(defn delete-and-move-left [editor]
  (let [node-loc (editor/find-node-loc editor (editor/get-editing editor))
        left-loc (zip/left node-loc)]
    (if (zip-utils/valid-loc? left-loc)
      (-> editor
        (stop-editing)
        (set-cursor-to-node-if-exists (zip-utils/loc-id left-loc)))
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

(defn delete-linebreak-or-token-after-cursor [editor]
  {:pre [(not (editor/editing? editor))]}
  (let [cursor-id (editor/get-cursor editor)]
    (editor/remove-right-siblink editor cursor-id)))

(defn delete-linebreak-or-token-before-cursor [editor]
  {:pre [(not (editor/editing? editor))]}
  (let [cursor-id (editor/get-cursor editor)]
    (editor/remove-left-siblink editor cursor-id)))

(defn open-list [editor]
  (let [placeholder-node (editor/prepare-placeholder-node)
        list-node (editor/prepare-list-node [placeholder-node])]
    (-> editor
      (insert-and-start-editing (:id placeholder-node) list-node))))
