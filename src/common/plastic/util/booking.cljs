(ns plastic.util.booking
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [reagent.ratom :refer [IDisposable dispose!]]))

; -------------------------------------------------------------------------------------------------------------------

(defn make-booking []
  (atom {}))

(defn register-item! [booking id value]
  (swap! booking assoc id value))

(defn unregister-item! [booking id]
  (swap! booking dissoc id))

(defn get-item [booking id]
  (get @booking id))

(defn read [booking id f & args]
  (apply f (get-item booking id) args))

(defn update-item! [booking id f & args]
  (swap! booking (fn [state] (apply update state id f args))))
