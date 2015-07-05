(ns quark.util.helpers
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (let [inputs (remove nil? vals)
        res (if (every? map? inputs)
              (apply merge-with deep-merge inputs)
              (last vals))]
    ;(log "DM" inputs "===>" res)
    res))