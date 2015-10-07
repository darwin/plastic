(ns meld.util
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]))

; -------------------------------------------------------------------------------------------------------------------

(defn update! [m k f & args]
  (let [o (get m k)]
    (assoc! m k (apply f o args))))

(defn dissoc-all! [m& keys]
  (reduce dissoc! m& keys))

(defn transplant-meta [new old]
  (with-meta new (meta old)))

(defn indexed-iteration [coll]
  {:pre [(coll? coll)]}
  (map-indexed (fn [i v] [i v]) coll))

(defn indexed-react-keys [coll]
  (map (fn [[index item]] (with-meta item {:key index})) (indexed-iteration coll)))

(defn select-values [pred map]
  (let [keys (keep (fn [[k v]] (if (pred v) k)) map)]
    (select-keys map keys)))

(def remove-nil-keys (partial select-values (complement nil?)))