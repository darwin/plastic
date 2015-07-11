(ns plastic.cogs.editor.layout.analysis.selectables
  (:require [rewrite-clj.node :as node]
            [plastic.cogs.editor.layout.utils :as utils])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

(defn resolve-symbol [[id node]]
  (if (utils/is-selectable? (node/tag node))
    {id {:selectable? true}}
    {id {}}))

(defn analyze-selectables [nodes analysis]
  (merge analysis (into {} (map resolve-symbol nodes))))
