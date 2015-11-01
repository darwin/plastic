(ns plastic.worker.frame.db
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [reagent.core :as reagent]
            [plastic.env :as env :include-macros true]
            [plastic.worker.validator :as validator]))

; -------------------------------------------------------------------------------------------------------------------

(def defaults
  {:undo-redo {}})

(defn provide-validator [context]
  (if (env/or context :validate-dbs :validate-worker-db)
    (validator/create context)))

(defn make-validator [context]
  (or (provide-validator context) (constantly true)))

(defn make-db [context]
  (reagent/atom defaults :validator (make-validator context)))