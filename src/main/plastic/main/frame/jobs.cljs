(ns plastic.main.frame.jobs
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [reagent.core :as reagent]
            [plastic.main.db :refer [db]]
            [re-frame.middleware :as middleware]
            [re-frame.frame :as frame]
            [re-frame.scaffold :as scaffold]
            [re-frame.utils :as utils]))

(defonce ^:dynamic *jobs* {})

(defn register-job [job-id continuation]
  (set! *jobs* (assoc *jobs* job-id {:events       []
                                     :continuation continuation})))

(defn unregister-job [job-id]
  (set! *jobs* (dissoc *jobs* job-id)))

(defn buffer-job-event [job-id event]
  (set! *jobs* (update-in *jobs* [job-id :events] (fn [events] (conj events event)))))

(defn get-job [job-id]
  {:pre [job-id]
   :post [%]}
  (get *jobs* job-id))

(defn events [job]
  (:events job))

(defn continuation [job]
  (:continuation job))