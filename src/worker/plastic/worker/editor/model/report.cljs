(ns plastic.worker.editor.model.report
  (:refer-clojure :exclude [merge])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]))

; -------------------------------------------------------------------------------------------------------------------
; a report is used to collect information on nodes affected by rewriting operation
; for UI highlighting purposes

(defn make
  ([] (make [] [] [] []))
  ([modified added removed moved]
   {:pre [(vector? modified)
          (vector? added)
          (vector? removed)
          (vector? moved)]}
   {:modified modified
    :added    added
    :removed  removed
    :moved    moved}))

(defn make-modified [node-id]
  (make [node-id] [] [] []))

(defn make-added [node-id]
  (make [] [node-id] [] []))

(defn make-removed [node-id]
  (make [] [] [node-id] []))

(defn make-moved [node-id]
  (make [] [] [] [node-id]))

(defn make-modified-list [ids]
  (make (vec ids) [] [] []))

(defn make-added-list [ids]
  (make [] (vec ids) [] []))

(defn make-removed-list [ids]
  (make [] [] (vec ids) []))

(defn make-moved-list [ids]
  (make [] [] [] (vec ids)))

(defn merge [report new-report]
  (make
    (vec (concat (:modified report) (:modified new-report)))
    (vec (concat (:added report) (:added new-report)))
    (vec (concat (:removed report) (:removed new-report)))
    (vec (concat (:moved report) (:moved new-report)))))
