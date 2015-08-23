(ns plastic.worker.db
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [reagent.core :as reagent]))

(def defaults
  {:undo-redo {}})

;; -- Application State  --------------------------------------------------------------------------------------------
;;
;; Should not be accessed directly by application code
;; Read access goes through subscriptions.
;; Updates via event handlers.
(def db (reagent/atom defaults))
