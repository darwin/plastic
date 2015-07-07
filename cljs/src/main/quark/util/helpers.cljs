(ns quark.util.helpers
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]])
  (:require [cljs.pprint :as pprint :refer [pprint]]))

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (let [inputs (remove nil? vals)
        res (if (every? map? inputs)
              (apply merge-with deep-merge inputs)
              (last vals))]
    ;(log "DM" inputs "===>" res)
    res))

; --------------------------------------------------------
;

(defn- type-dispatcher [obj]
  (cond
    (fn? obj) :fn
    :default :default))

(defmulti clean-dispatch type-dispatcher)

(defmethod clean-dispatch :default [thing]
  (pprint/simple-dispatch thing))

(defmethod clean-dispatch :fn []
  (-write cljs.core/*out* "..."))

(defn nice-print [o]
  (with-out-str
    (pprint/with-pprint-dispatch clean-dispatch
      (binding [pprint/*print-pretty* true
                pprint/*print-suppress-namespaces* true
                pprint/*print-lines* true]
        (pprint o)))))
