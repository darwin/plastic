(ns plastic.worker.editor.xforms.editing
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.editor.model :as editor]
            [plastic.worker.editor.toolkit.id :as id]
            [rewrite-clj.node.keyword :refer [keyword-node]]
            [rewrite-clj.node :as node]
            [plastic.worker.editor.parser.utils :as parser]))

(defn insert-and-start-editing [editor node-id & values]
  (if-not (id/spot? node-id)
    (editor/insert-values-after-node editor node-id values)
    (editor/insert-values-before-first-child-of-node editor node-id values)))

(defn postprocess-text-after-editing [editor-mode text]
  (condp = editor-mode
    :symbol (node/coerce (symbol text))
    :keyword (keyword-node (keyword text))                                                                            ; TODO: investigate - coerce does not work for keywords?
    :string (node/coerce text)
    (throw "unknown editor mode in postprocess-text-after-editing:" editor-mode)))

(defn edit-node [editor node-id [editor-mode text]]
  (let [node-value (parser/assoc-node-id (postprocess-text-after-editing editor-mode text))]
    (editor/commit-node-value editor node-id node-value)))

(defn enter [editor edit-point]
  (let [placeholder-node (editor/prepare-placeholder-node)]
    (insert-and-start-editing editor edit-point (editor/prepare-newline-node) placeholder-node)))

(defn alt-enter [editor edit-point]
  (editor/remove-linebreak-before-node editor edit-point))

(defn space [editor edit-point]
  (let [placeholder-node (editor/prepare-placeholder-node)]
    (insert-and-start-editing editor edit-point placeholder-node)))

(defn backspace [editor edit-point]
  (editor/delete-node editor edit-point))

(defn delete [editor edit-point]
  (if (id/spot? edit-point)
    (editor/remove-first-child-of-node editor edit-point)
    (editor/remove-right-siblink editor edit-point)))

(defn alt-delete [editor edit-point]
  (editor/remove-left-siblink editor edit-point))

(defn open-compound [editor edit-point node-prepare-fn]
  (let [placeholder-node (editor/prepare-placeholder-node)
        compound-node (node-prepare-fn [placeholder-node])]
    (insert-and-start-editing editor edit-point compound-node)))

(defn open-list [editor edit-point]
  (open-compound editor edit-point editor/prepare-list-node))

(defn open-vector [editor edit-point]
  (open-compound editor edit-point editor/prepare-vector-node))

(defn open-map [editor edit-point]
  (open-compound editor edit-point editor/prepare-map-node))

(defn open-set [editor edit-point]
  (open-compound editor edit-point editor/prepare-set-node))

(defn open-fn [editor edit-point]
  (open-compound editor edit-point editor/prepare-fn-node))

(defn open-meta [editor edit-point]
  (let [placeholder-node (editor/prepare-placeholder-node)
        temporary-meta-data (editor/prepare-keyword-node :meta)
        compound-node (editor/prepare-meta-node [temporary-meta-data placeholder-node])]
    (insert-and-start-editing editor edit-point compound-node)))

(defn open-quote [editor edit-point]
  (open-compound editor edit-point editor/prepare-quote-node))

(defn open-deref [editor edit-point]
  (open-compound editor edit-point editor/prepare-deref-node))

(defn insert-placeholder-as-first-child [editor edit-point]
  (let [placeholder-node (editor/prepare-placeholder-node)]
    (editor/insert-values-before-first-child-of-node editor edit-point [placeholder-node])))
