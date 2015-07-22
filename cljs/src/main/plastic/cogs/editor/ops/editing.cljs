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

(defn should-commit? [editor-id]
  (and (onion/is-inline-editor-modified? editor-id)))

; ----------------------------------------------------------------------------------------------------------------

(defn start-editing [editor]
  (let [cursor-id (editor/get-cursor editor)]
    (if (id/spot? cursor-id)
      (let [placeholder-node (editor/prepare-placeholder-node)]
        (-> editor
          (editor/insert-values-before-first-child-of-node cursor-id [placeholder-node])
          (editor/set-cursor (:id placeholder-node))
          (editor/set-editing (:id placeholder-node))))
      (editor/set-editing editor (if (editor/can-edit-cursor? editor) cursor-id)))))

(defn stop-editing [editor]
  (or
    (if (editor/editing? editor)
      (let [editor-id (editor/get-id editor)
            editor-after-commit (if (should-commit? editor-id)
                                  (commit-value editor (onion/get-postprocessed-value-after-editing editor-id))
                                  editor)]
        (editor/set-editing editor-after-commit nil)))
    editor))

(defn insert-and-start-editing [editor node-id-to-be-edited & values]
  (let [current-node-id (if (editor/editing? editor) (editor/get-editing editor) (editor/get-cursor editor))
        editor-after-insertion (if-not (id/spot? current-node-id)
                                 (editor/insert-values-after-node editor current-node-id values)
                                 (editor/insert-values-before-first-child-of-node editor current-node-id values))]
    (-> editor-after-insertion
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
  {:pre [(not (editing-string? editor))]}
  (let [placeholder-node (editor/prepare-placeholder-node)]
    (insert-and-start-editing editor (:id placeholder-node) (editor/prepare-newline-node) placeholder-node)))

(defn alt-enter [editor]
  {:pre [(not (editing-string? editor))]}
  (editor/remove-linebreak-before-node editor (editor/get-cursor editor)))

(defn space [editor]
  {:pre [(not (editing-string? editor))]}
  (let [placeholder-node (editor/prepare-placeholder-node)]
    (insert-and-start-editing editor (:id placeholder-node) placeholder-node)))

(defn delete-node-before-cursor [editor]
  {:pre [(not (editor/editing? editor))]}
  (if-let [cursor-id (editor/get-cursor editor)]
    (let [editor-with-next-selection-and-focus (select-next-thing-for-case-of-selected-node-removal editor)
          next-cursor-id (editor/get-cursor editor-with-next-selection-and-focus)]
      (-> editor
        (editor/delete-node cursor-id)
        (set-cursor-to-node-if-exists next-cursor-id)))
    editor))

(defn stop-editing-and-move-left-or-up [editor]
  {:pre [(onion/is-inline-editor-empty? (editor/get-id editor))]}
  (let [node-loc (editor/find-node-loc editor (editor/get-editing editor))
        left-loc (zip/left node-loc)]
    (if (zip-utils/valid-loc? left-loc)
      (-> editor
        (stop-editing)
        (set-cursor-to-node-if-exists (zip-utils/loc-id left-loc)))
      (-> editor
        (stop-editing)
        (selection/structural-up)))))

(defn delete-linebreak-or-token-after-cursor [editor]
  {:pre [(not (editor/editing? editor))]}
  (let [cursor-id (editor/get-cursor editor)]
    (if (id/spot? cursor-id)
      (editor/remove-first-child-of-node editor cursor-id)
      (editor/remove-right-siblink editor cursor-id))))

(defn delete-linebreak-or-token-before-cursor [editor]
  {:pre [(not (editor/editing? editor))]}
  (let [cursor-id (editor/get-cursor editor)]
    (editor/remove-left-siblink editor cursor-id)))

(defn open-compound [editor node-prepare-fn]
  (let [placeholder-node (editor/prepare-placeholder-node)
        compound-node (node-prepare-fn [placeholder-node])]
    (insert-and-start-editing editor (:id placeholder-node) compound-node)))

(defn open-list [editor]
  (open-compound editor editor/prepare-list-node))

(defn open-vector [editor]
  (open-compound editor editor/prepare-vector-node))

(defn open-map [editor]
  (open-compound editor editor/prepare-map-node))

(defn open-set [editor]
  (open-compound editor editor/prepare-set-node))

(defn open-fn [editor]
  (open-compound editor editor/prepare-fn-node))

(defn open-meta [editor]
  (let [placeholder-node (editor/prepare-placeholder-node)
        temporary-meta-data (editor/prepare-keyword-node :meta)
        compound-node (editor/prepare-meta-node [temporary-meta-data placeholder-node])]
    (insert-and-start-editing editor (:id placeholder-node) compound-node)))

(defn open-quote [editor]
  (open-compound editor editor/prepare-quote-node))

(defn open-deref [editor]
  (open-compound editor editor/prepare-deref-node))
