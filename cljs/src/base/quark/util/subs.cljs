(ns quark.util.subs
  (:require [reagent.ratom])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [reagent.ratom :refer [reaction]]))

(defn get-in-query-factory [path]
  (fn [db [_query-id]]
    (reaction (get-in @db path))))