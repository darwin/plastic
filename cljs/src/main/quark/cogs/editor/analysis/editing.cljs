(ns quark.cogs.editor.analysis.editing
  (:require [rewrite-clj.node :as node]
            [quark.cogs.editor.utils :refer [is-selectable? prepare-string-for-display ancestor-count loc->path leaf-nodes make-zipper collect-all-right]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn process-node [edited nodes record]
  (let [[id info] record
        node (get nodes id)]
    (if (is-selectable? (node/tag node))
      (if (contains? edited id)
        {id (assoc info :editing? true)}
        record)
      record)))

(defn analyze-editing [editing nodes analysis]
  {:pre [(set? editing)]}
  (if (empty? editing)
    analysis
    (into {} (map (partial process-node editing nodes) analysis))))
