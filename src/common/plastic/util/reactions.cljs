(ns plastic.util.reactions
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [reagent.ratom :as reagent]))

; -------------------------------------------------------------------------------------------------------------------

(defn dispose-reaction! [reaction]
  (reagent/dispose! reaction))

; -------------------------------------------------------------------------------------------------------------------

(defn register-reaction [storage reaction]
  (update storage :reactions (fn [reactions] (conj reactions reaction))))

(defn unregister-reaction [storage reaction]
  (update storage :reactions (fn [reactions] (remove #(identical? reaction %) reactions))))

(defn unregister-and-dispose-all-reactions! [storage]
  (doseq [reaction (:reactions storage)]
    (dispose-reaction! reaction))
  (dissoc storage :reactions))