(ns plastic.worker.editor.model.xforming
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.zip :as zip-utils]
            [clojure.zip :as z]
            [plastic.worker.editor.model.report :as report]
            [plastic.worker.editor.model :as editor :refer [valid-editor?]]))

(defn make-state [loc report]
  [loc report])

(defn get-loc [state]
  (first state))

(defn get-report [state]
  (second state))

(defn make-initial-state [editor]
  {:pre [(valid-editor? editor)]}
  (let [parse-tree (editor/get-parse-tree editor)
        root-loc (zip-utils/make-zipper parse-tree)]
    (make-state root-loc (report/make))))

(defn commit-state [editor state]
  {:pre [(valid-editor? editor)]}
  (if (nil? state)
    editor
    (let [parse-tree (z/root (get-loc state))
          report (get-report state)]
      (-> editor
        (editor/set-xform-report report)
        (editor/set-parse-tree parse-tree)))))

; -------------------------------------------------------------------------------------------------------------------

(defn apply-ops [editor f coll]
  {:pre [(valid-editor? editor)]}
  (let [initial-state (make-initial-state editor)
        state (reduce f initial-state coll)]
    (commit-state editor state)))

(defn apply-op [editor zip-op & args]
  {:pre [(valid-editor? editor)]}
  (apply-ops editor #(apply %2 (conj (vec args) %1)) [zip-op]))
