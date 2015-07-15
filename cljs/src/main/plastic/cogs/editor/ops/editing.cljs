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

(defn set-selection-to-node-with-sticker-if-still-exists [editor sticker]
  (let [commit-node-loc (editor/find-node-loc-with-sticker editor sticker)]
    (if (zip-utils/valid-loc? commit-node-loc)
      (editor/set-selection editor #{(editor/loc-id commit-node-loc)})
      editor)))

(defn commit-value [editor value]
  (let [node-id (get-edited-node-id editor)]
    (-> editor
      (editor/add-sticker-on-node node-id :commit-node)
      (select-next-candidate-for-case-of-selected-node-removal)
      (editor/commit-node-value node-id value)
      (set-selection-to-node-with-sticker-if-still-exists :commit-node)
      (editor/remove-sticker :commit-node))))

(defn apply-editing [editor action]
  (let [should-be-editing? (= action :start)
        can-edit? (editor/can-edit-selection? editor)]
    (editor/set-editing-set editor (if (and can-edit? should-be-editing?) (editor/get-selection editor)))))

(defn insert-values-after-point [editor point values]
  (let [point-node (editor/find-node-with-sticker editor point)]
    (editor/insert-values-after-node editor (:id point-node) values)))

(defn insert-values-before-point [editor point values]
  (let [point-node (editor/find-node-with-sticker editor point)]
    (editor/insert-values-before-node editor (:id point-node) values)))

; ----------------------------------------------------------------------------------------------------------------

(defn start-editing [editor]
  (apply-editing editor :start))

(defn stop-editing [editor]
  (or
    (if (editor/editing? editor)
      (let [editor-id (editor/get-id editor)
            modified-editor (if (or true (onion/is-inline-editor-modified? editor-id))
                              (commit-value editor (onion/get-postprocessed-value-after-editing editor-id))
                              (do (log "not modified")
                                  editor))]
        (dom/postpone-selection-overlay-display-until-next-update editor)
        (apply-editing modified-editor :stop)))
    editor))

(defn insert-and-start-editing [editor sticker & values]
  (let [editor-after-stop-editing (if (editor/editing? editor) (stop-editing editor) editor)
        node-id (if (editor/editing? editor) (get-edited-node-id editor) (get-selected-node-id editor))]
    (-> editor-after-stop-editing
      (editor/insert-values-after-node node-id values)
      (set-selection-to-node-with-sticker-if-still-exists sticker)
      (editor/remove-sticker sticker)
      (start-editing))))

(defn prepend-and-keep-selection [editor & values]
  (let [selected-id (get-selected-node-id editor)]
    (-> editor
      (editor/add-sticker-on-node selected-id :selected)
      (editor/insert-values-before-node selected-id values)
      (set-selection-to-node-with-sticker-if-still-exists :selected)
      (editor/remove-sticker :selected))))

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
      (insert-and-start-editing editor :placeholder (editor/prepare-newline-node) (editor/node-add-sticker (editor/prepare-placeholder-node) :placeholder))
      (prepend-and-keep-selection editor (editor/prepare-newline-node)))))

(defn space [editor]
  (if (editing-string-or-doc? editor)
    (insert-text-into-inline-editor editor " ")
    (insert-and-start-editing editor :placeholder (editor/node-add-sticker (editor/prepare-placeholder-node) :placeholder))))

(defn delete-selection [editor]
  {:pre [(not (editor/editing? editor))]}
  (let [node-id (get-selected-node-id editor)
        editor-with-next-selection (select-next-candidate-for-case-of-selected-node-removal editor)
        next-selection-node-id (get-selected-node-id editor-with-next-selection)]
    (-> editor
      (editor/add-sticker-on-node next-selection-node-id :edit-point)
      (editor/delete-node node-id)
      (set-selection-to-node-with-sticker-if-still-exists :edit-point)
      (editor/remove-sticker :edit-point))))

(defn delete-and-move-left [editor]
  (let [node-loc (editor/find-node-loc editor (get-edited-node-id editor))
        left-loc (zip/left node-loc)]
    (if (zip-utils/valid-loc? left-loc)
      (-> editor
        (editor/add-sticker-on-node (editor/loc-id left-loc) :left-point)
        (stop-editing)
        (set-selection-to-node-with-sticker-if-still-exists :left-point)
        (editor/remove-sticker :left-point))
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