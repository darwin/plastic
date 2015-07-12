(ns plastic.util.subs
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [reagent.ratom :refer [reaction]])
  (:require [reagent.ratom]))

(defn path-query-factory [path-or-fn]
  (fn [db [_query-id & args]]
    (reaction (get-in @db (if (fn? path-or-fn)
                            (apply path-or-fn args)
                            path-or-fn)))))