(ns plastic.worker.editor.model.rewriting
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.zip :as zip-utils]
            [clojure.zip :as z]
            [plastic.worker.editor.model.zipping :as zipping]
            [plastic.worker.editor.model :as editor :refer [valid-editor?]]
            [plastic.worker.editor.toolkit.id :as id]))

; also see 'xforms' folder for more high-level editor transformations

(defn transform-parse-tree [editor transformation]
  {:pre [(valid-editor? editor)]}
  (let [new-parse-tree (transformation (editor/get-parse-tree editor))]
    (editor/set-parse-tree editor new-parse-tree)))

(defn zipping-parse-tree-transformer [operation]
  (fn [parse-tree]
    (if-let [result (operation (zip-utils/make-zipper parse-tree))]
      (z/root result)
      parse-tree)))

(defn transform-parse-tree-via-zipping [editor operation]
  (transform-parse-tree editor (zipping-parse-tree-transformer operation)))

; -------------------------------------------------------------------------------------------------------------------

(defn commit-node-value [editor node-id value]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree-via-zipping editor
    (fn [root-loc]
      (zipping/commit-value root-loc node-id value))))

(defn delete-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree-via-zipping editor
    (partial zipping/delete-node node-id)))

(defn insert-values-after [editor node-id values]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree-via-zipping editor
    (partial zipping/insert-values-after node-id values)))

(defn insert-values-before [editor node-id values]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree-via-zipping editor
    (partial zipping/insert-values-before node-id values)))

(defn insert-values-before-first-child [editor node-id values]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree-via-zipping editor
    (partial zipping/insert-values-before-first-child node-id values)))

(defn remove-linebreak-before [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree-via-zipping editor
    (partial zipping/remove-linebreak-before node-id)))

(defn remove-right-siblink [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree-via-zipping editor
    (partial zipping/remove-right-siblink node-id)))

(defn remove-left-siblink [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree-via-zipping editor
    (partial zipping/remove-left-siblink node-id)))

(defn remove-first-child [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree-via-zipping editor
    (partial zipping/remove-first-child node-id)))
