(ns plastic.worker.editor.xforms.rewriting
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.editor.xforms.zipops :as ops]))

(defn commit-node-value [node-id value]
  [[ops/lookup node-id]
   [ops/commit value]])

(defn delete-node [node-id]
  [[ops/lookup node-id]
   [ops/remove-whitespaces-and-newlines-before]
   [ops/remove]])

(defn insert-values-after [node-id values]
  [[ops/lookup node-id]
   [ops/insert-after values]])

(defn insert-values-before [node-id values]
  [[ops/lookup node-id]
   [ops/insert-before values]])

(defn insert-values-before-first-child [node-id values]
  [[ops/lookup node-id]
   [ops/step-down]
   [ops/insert-before values]])

(defn remove-linebreak-before [node-id]
  [[ops/lookup node-id]
   [ops/remove-linebreak-before]])

(defn remove-right-siblink [node-id]
  [[ops/lookup node-id]
   [ops/step-right]
   [ops/remove]])

(defn remove-left-siblink [node-id]
  [[ops/lookup node-id]
   [ops/step-left]
   [ops/remove]
   [ops/step-next]])

(defn remove-first-child [node-id]
  [[ops/lookup node-id]
   [ops/step-down]
   [ops/remove]])
