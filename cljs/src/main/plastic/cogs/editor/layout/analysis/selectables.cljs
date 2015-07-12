(ns plastic.cogs.editor.layout.analysis.selectables
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.node :as node]
            [plastic.cogs.editor.layout.utils :as layout-utils]))

(defn resolve-symbol [[id node]]
  (if (layout-utils/is-selectable? (node/tag node))
    {id {:selectable? true}}
    {id {}}))

(defn analyze-selectables [nodes analysis]
  (merge analysis (into {} (map resolve-symbol nodes))))
