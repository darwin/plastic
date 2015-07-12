(ns plastic.cogs.editor.layout.analysis.editing
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

(defn process-node [editing-set node-analysis]
  (let [[id info] node-analysis]
    (if (:selectable? info)
      (if (contains? editing-set id)
        {id (assoc info :editing? true)}
        node-analysis)
      node-analysis)))

(defn analyze-editing [editing-set analysis]
  {:pre [(set? editing-set)]}
  (if (empty? editing-set)
    analysis
    (into {} (map (partial process-node editing-set) analysis))))
