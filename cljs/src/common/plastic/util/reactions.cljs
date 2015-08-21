(ns plastic.util.reactions
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [reagent.ratom :refer [IDisposable dispose!]]))

(defn register-reaction [storage reaction]
  (update storage :reactions (fn [reactions] (conj reactions reaction))))

(defn dispose-reactions! [storage]
  (doseq [reaction (:reactions storage)]
    (dispose! reaction))
  (dissoc storage :reactions))
