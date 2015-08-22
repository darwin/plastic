(ns plastic.worker.editor.model.rewriting
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [plastic.util.zip :as zip-utils]
            [clojure.zip :as z]
            [plastic.worker.editor.model.zipping :as zipping]
            [plastic.worker.editor.model :as editor :refer [valid-editor?]]
            [plastic.worker.editor.toolkit.id :as id]))

; also see 'xforms' folder for more high-level editor transformations

(defn transform-parse-tree [editor transformation]
  {:pre [(valid-editor? editor)]}
  (let [new-parse-tree (transformation (editor/get-parse-tree editor))]
    (editor/set-parse-tree editor new-parse-tree)))

(defn parse-tree-transformer [f]
  (fn [parse-tree]
    (if-let [result (f (zip-utils/make-zipper parse-tree))]
      (z/root result)
      parse-tree)))

; -------------------------------------------------------------------------------------------------------------------

(defn commit-node-value [editor node-id value]
  {:pre [(valid-editor? editor)
         node-id
         value]}
  (let [old-root (editor/get-parse-tree editor)
        root-loc (zip-utils/make-zipper old-root)
        modified-loc (zipping/commit-value-to-loc root-loc node-id value)]
    (if-not modified-loc
      editor
      (editor/set-parse-tree editor (zip/root modified-loc)))))

(defn delete-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor (parse-tree-transformer (partial zipping/delete-node-loc (id/id-part node-id)))))

(defn insert-values-after-node [editor node-id values]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial zipping/insert-values-after-node-loc (id/id-part node-id) values))))

(defn insert-values-before-node [editor node-id values]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial zipping/insert-values-before-node-loc (id/id-part node-id) values))))

(defn insert-values-before-first-child-of-node [editor node-id values]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial zipping/insert-values-before-first-child-of-node-loc (id/id-part node-id) values))))

(defn remove-linebreak-before-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial zipping/remove-linebreak-before-node-loc (id/id-part node-id)))))

(defn remove-right-siblink [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial zipping/remove-right-siblink-of-loc (id/id-part node-id)))))

(defn remove-left-siblink [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial zipping/remove-left-siblink-of-loc (id/id-part node-id)))))

(defn remove-first-child-of-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial zipping/remove-first-child-of-node-loc (id/id-part node-id)))))
