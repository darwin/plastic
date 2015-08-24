(ns plastic.worker.db
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [reagent.core :as reagent]
            [plastic.worker.validator :as validator]))

(def defaults
  {:undo-redo {}})

;; -- Application State  --------------------------------------------------------------------------------------------
;;
;; Should not be accessed directly by application code
;; Read access goes through subscriptions.
;; Updates via event handlers.

(defn provide-validator []
  (if (or plastic.env.validate-dbs plastic.env.validate-worker-db)
    (validator/create)))

(def db (reagent/atom defaults :validator (provide-validator)))
