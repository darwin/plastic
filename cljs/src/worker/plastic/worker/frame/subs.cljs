(ns plastic.worker.frame.subs
  (:require [plastic.worker.db :refer [db]]
            [plastic.worker.frame.utils :refer [first-in-vector]])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.common :refer [defonce]]))


;; maps from handler-id to handler-fn
(defonce ^:private key->fn (atom {}))


(defn clear-handlers!
  "Unregisters all subscription handlers"
  []
  (reset! key->fn {}))


(defn register
  "Registers a handler function for an id"
  [key-v handler-fn]
  (if (contains? @key->fn key-v)
    (warn "re-frame: overwriting subscription-handler for: " key-v)) ;; allow it, but warn.
  (swap! key->fn assoc key-v handler-fn))


(defn subscribe
  "Returns a reagent/reaction which observes a part of app-db"
  [db v]
  (let [key-v (first-in-vector v)
        handler-fn (get @key->fn key-v)]
    (if (nil? handler-fn)
      (error "re-frame: no subscription handler registered for: \"" key-v "\".  Returning a nil subscription.")
      (handler-fn db v))))
