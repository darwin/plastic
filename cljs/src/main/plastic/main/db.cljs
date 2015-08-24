(ns plastic.main.db
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [reagent.core :as reagent]
            [plastic.main.validator :as validator]))

(def defaults
  {:settings  {:headers-visible true
               :code-visible    true
               :docs-visible    true}
   :undo-redo {}})

;; -- Application State  --------------------------------------------------------------------------------------------
;;
;; Should not be accessed directly by application code
;; Read access goes through subscriptions.
;; Updates via event handlers.

(defn provide-validator []
  (if (or plastic.env.validate-dbs plastic.env.validate-main-db)
    (validator/create)))

(def db (reagent/atom defaults :validator (provide-validator)))
