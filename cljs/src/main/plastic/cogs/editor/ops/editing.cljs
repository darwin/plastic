(ns plastic.cogs.editor.ops.editing
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.render.dom :as dom]
            [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.cogs.editor.model :as editor]
            [plastic.cogs.editor.ops.selection :as selection]
            [plastic.onion.core :as onion]
            [plastic.util.zip :as zip-utils]))

(defn select-next-candidate-for-case-of-selected-node-removal [editor]
  (selection/apply-move-selection editor :spatial-left :spatial-right :structural-up))

(defn get-selected-node-id [editor]
  (first (editor/get-focused-selection editor)))

(defn get-edited-node-id [editor]
  (first (editor/get-editing-set editor)))

(defn set-selection-to-node-with-sticker-if-still-exists [editor sticker]
  (let [commit-node-loc (editor/find-node-loc-with-sticker editor sticker)]
    (if (zip-utils/valid-loc? commit-node-loc)
      (editor/set-focused-selection editor #{(editor/loc-id commit-node-loc)})
      editor)))

(defn commit-value [editor value]
  (let [node-id (get-edited-node-id editor)]
    (-> editor
      (editor/add-sticker-on-node node-id :commit-node)
      (select-next-candidate-for-case-of-selected-node-removal)
      (editor/commit-node-value node-id value)
      (set-selection-to-node-with-sticker-if-still-exists :commit-node)
      (editor/remove-sticker :commit-node))))

(defn apply-editing [editor op]
  (let [should-be-editing? (= op :start)
        can-edit? (editor/can-edit-focused-selection? editor)]
    (editor/set-editing-set editor (if (and can-edit? should-be-editing?) (editor/get-focused-selection editor)))))

(defn insert-values-after-edit-point [editor values]
  (let [edit-point-node (editor/find-node-with-sticker editor :edit-point)]
    (editor/insert-values-after-node editor (:id edit-point-node) values)))

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

(defn insert-and-continue-editing [editor & values]
  {:pre [(editor/editing? editor)]}
  (-> editor
    (editor/add-sticker-on-node (get-edited-node-id editor) :edit-point)
    (stop-editing)
    (insert-values-after-edit-point values)
    (set-selection-to-node-with-sticker-if-still-exists :placeholder)
    (editor/remove-sticker :placeholder)
    (editor/remove-sticker :edit-point)
    (start-editing)))

(defn enter [editor]
  (insert-and-continue-editing editor (editor/prepare-newline-node) (editor/node-add-sticker (editor/prepare-placeholder-node) :placeholder)))

(defn space [editor]
  (insert-and-continue-editing editor (editor/node-add-sticker (editor/prepare-placeholder-node) :placeholder)))

(defn delete-selection [editor]
  {:pre [(not (editor/editing? editor))]}
  (let [node-id (get-selected-node-id editor)]
    (-> editor
      (select-next-candidate-for-case-of-selected-node-removal)
      (editor/delete-node node-id))))