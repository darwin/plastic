(ns plastic.worker.editor.model.xforming
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.zip :as zip-utils]
            [clojure.zip :as z]
            [plastic.worker.editor.model.report :as report]
            [plastic.worker.editor.model :as editor :refer [valid-editor?]]))

(defn make-state [loc report]
  [loc report])

(defn make-initial-state [editor]
  {:pre [(valid-editor? editor)]}
  (let [parse-tree (editor/get-parse-tree editor)
        root-loc (zip-utils/make-zipper parse-tree)]
    (make-state root-loc (report/make))))

(defn op-reducer [state zip-op]
  (let [op-fn (first zip-op)
        op-args (conj (vec (rest zip-op)) state)
        op-res (apply op-fn op-args)]
    op-res))

(defn apply-ops [editor zip-ops]
  (let [initial-state (make-initial-state editor)
        final-state (reduce op-reducer initial-state zip-ops)
        final-parse-tree (z/root (first final-state))
        final-report (second final-state)]
    (-> editor
      (editor/set-xform-report final-report)
      (editor/set-parse-tree final-parse-tree))))

(defn chain-ops [& args]
  (apply concat args))
