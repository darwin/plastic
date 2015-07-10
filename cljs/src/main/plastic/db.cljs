(ns plastic.db
  (:require [reagent.core :as reagent]))

(def defaults
  {:settings
   {:code-visible true
    :docs-visible true}})

;; -- Application State  --------------------------------------------------------------------------
;;
;; Should not be accessed directly by application code
;; Read access goes through subscriptions.
;; Updates via event handlers.
(def app-db (reagent/atom defaults))