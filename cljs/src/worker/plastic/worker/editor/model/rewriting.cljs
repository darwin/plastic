(ns plastic.worker.editor.model.rewriting
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.zip :as zip-utils]
            [clojure.zip :as z]
            [plastic.worker.editor.model.zipping :as zipping]
            [plastic.worker.editor.model.zipops :as zipops]
            [plastic.worker.editor.model.xforming :as transforming]
            [plastic.worker.editor.model :as editor :refer [valid-editor?]]
            [plastic.worker.editor.toolkit.id :as id]))

(defn commit-node-value [editor node-id value]
  {:pre [(valid-editor? editor)]}
  (transforming/apply-zipops editor
    [[zipops/find node-id]
     [zipops/commit value]]))

(defn delete-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transforming/apply-zipops editor
    [[zipops/find node-id]
     [zipops/remove-whitespaces-and-newlines-before]
     [zipops/remove]]))

(defn insert-values-after [editor node-id values]
  {:pre [(valid-editor? editor)]}
  (transforming/apply-zipops editor
    [[zipops/find node-id]
     [zipops/insert-after values]]))

(defn insert-values-before [editor node-id values]
  {:pre [(valid-editor? editor)]}
  (transforming/apply-zipops editor
    [[zipops/find node-id]
     [zipops/insert-before values]]))

(defn insert-values-before-first-child [editor node-id values]
  {:pre [(valid-editor? editor)]}
  (transforming/apply-zipops editor
    [[zipops/find node-id]
     [zipops/step-down]
     [zipops/insert-before values]]))

(defn remove-linebreak-before [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transforming/apply-zipops editor
    [[zipops/find node-id]
     [zipops/remove-linebreak-before]]))

(defn remove-right-siblink [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transforming/apply-zipops editor
    [[zipops/find node-id]
     [zipops/step-right]
     [zipops/remove]]))

(defn remove-left-siblink [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transforming/apply-zipops editor
    [[zipops/find node-id]
     [zipops/step-left]
     [zipops/remove]]))

(defn remove-first-child [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transforming/apply-zipops editor
    [[zipops/find node-id]
     [zipops/step-down]
     [zipops/remove]]))
