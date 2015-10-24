(ns plastic.worker.frame.db
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [reagent.core :as reagent]
            [plastic.env :as env :include-macros true]
            [plastic.worker.validator :as validator]))

; -------------------------------------------------------------------------------------------------------------------

(def defaults
  {:undo-redo {}})

(defn provide-validator [context]
  (if (env/or context :validate-dbs :validate-worker-db)
    (validator/create context)))

(defn make-validator [context]
  (or (provide-validator context) (fn [] true)))

(defn make-db [context]
  (reagent/atom defaults :validator (make-validator context)))