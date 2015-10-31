(ns plastic.main.frame.db
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [reagent.core :as reagent]
            [plastic.env :as env :include-macros true]
            [plastic.main.validator :as validator]))

; -------------------------------------------------------------------------------------------------------------------

(defonce defaults
  {:settings  {:headers-visible  true
               :docs-visible     true
               :code-visible     true
               :comments-visible true}
   :undo-redo {}})

(defn provide-validator [context]
  (if (env/or context :validate-dbs :validate-main-db)
    (validator/create context)))

(defn make-validator [context]
  (or (provide-validator context) (constantly true)))

(defn make-db [context]
  (reagent/atom defaults :validator (make-validator context)))
