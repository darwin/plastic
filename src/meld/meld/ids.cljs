(ns meld.ids
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]))

(defn insert-id* [inserter ids marker-id new-id]
  (loop [ids ids
         res []]
    (if-let [id (first ids)]
      (if (identical? id marker-id)
        (recur (rest ids) (inserter res marker-id new-id))                                                            ; TODO: I think here we can concat the rest, didn't work for me
        (recur (rest ids) (conj res id)))
      res)))

(def insert-id-right (partial insert-id* (fn [coll marker-id new-id] (-> coll (conj marker-id) (conj new-id)))))
(def insert-id-left (partial insert-id* (fn [coll marker-id new-id] (-> coll (conj new-id) (conj marker-id)))))

(defn remove-id [ids id]
  (remove #{id} ids))

(defn prepend-id [ids id]
  (cons id ids))

(defn append-id [ids id]
  (conj ids id))

(defn replace-id [ids old-id new-id]
  (map (fn [id] (if (identical? id old-id) new-id id)) ids))
