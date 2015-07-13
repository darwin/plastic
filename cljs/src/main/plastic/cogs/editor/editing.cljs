(ns plastic.cogs.editor.editing
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.render.dom :as dom]
            [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.cogs.editor.model :as editor]
            [plastic.onion.core :as onion]
            [rewrite-clj.node :as node]))

(defn commit-value [editor value]
  (let [node-id (first (editor/get-editing-set editor))]
    (editor/commit-node-value editor node-id value)))

(defn apply-editing [editor op]
  (let [should-be-editing? (= op :start)
        can-edit? (editor/can-edit-focused-selection? editor)]
    (editor/set-editing-set editor (if (and can-edit? should-be-editing?) (editor/get-focused-selection editor)))))

(defn insert-values-after-edit-point [editor values]
  (let [edit-point-node (editor/find-node-with-sticker editor :edit-point)]
    (editor/insert-values-after-node editor (:id edit-point-node) values)))

(defn set-focused-selection-to-placeholder-node [editor]
  (let [placeholder-node (editor/find-node-with-sticker editor :placeholder)]
    (editor/set-focused-selection editor #{(:id placeholder-node)})))

(defn get-edited-node-id [editor]
  (first (editor/get-editing-set editor)))

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

(defn insert-and-edit [editor & values]
  {:pre [(editor/editing? editor)]}
  (-> editor
    (editor/set-node-sticker (get-edited-node-id editor) :edit-point)
    (stop-editing)
    (insert-values-after-edit-point values)
    (set-focused-selection-to-placeholder-node)
    (editor/remove-node-sticker :placeholder)
    (editor/remove-node-sticker :edit-point)
    (start-editing)))

(defn enter [editor]
  (insert-and-edit editor (editor/prepare-newline-node) (assoc (editor/prepare-placeholder-node) :sticker :placeholder)))

(defn space [editor]
  (insert-and-edit editor (assoc (editor/prepare-placeholder-node) :sticker :placeholder)))
