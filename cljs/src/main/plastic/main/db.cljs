(ns plastic.main.db
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [reagent.core :as reagent]))

(def defaults
  {:settings
   {:headers-visible true
    :code-visible true
    :docs-visible true}})

;; -- Application State  --------------------------------------------------------------------------
;;
;; Should not be accessed directly by application code
;; Read access goes through subscriptions.
;; Updates via event handlers.
(def db (reagent/atom defaults))