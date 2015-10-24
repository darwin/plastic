(ns plastic.main.editor.ops.cursor
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.editor.ops.movement.form :refer [form-movement]]
            [plastic.main.editor.ops.movement.interest :refer [interest-movement]]
            [plastic.main.editor.ops.movement.spatial :refer [spatial-movement]]
            [plastic.main.editor.ops.movement.structural :refer [structural-movemement]]))

; -------------------------------------------------------------------------------------------------------------------

(def movements
  {:spatial-up       (partial spatial-movement :up)
   :spatial-down     (partial spatial-movement :down)
   :spatial-left     (partial spatial-movement :left)
   :spatial-right    (partial spatial-movement :right)
   :structural-up    (partial structural-movemement :up)
   :structural-down  (partial structural-movemement :down)
   :structural-left  (partial structural-movemement :left)
   :structural-right (partial structural-movemement :right)
   :prev-form        (partial form-movement :prev)
   :next-form        (partial form-movement :next)
   :prev-interest    (partial interest-movement :prev)
   :next-interest    (partial interest-movement :next)})

(defn apply-moves [editor moves-to-try]
  (if-let [movement (first moves-to-try)]
    (if-let [op (movement movements)]
      (if-let [result (op editor)]
        result
        (recur editor (rest moves-to-try)))
      (error (str "Unknown movement '" movement "'")))))

(defn apply-move-cursor [editor & movements]
  (or (apply-moves editor movements) editor))

; -------------------------------------------------------------------------------------------------------------------

; spatial movement

(defn spatial-up [editor]
  (apply-move-cursor editor :spatial-up :prev-form))

(defn spatial-down [editor]
  (apply-move-cursor editor :spatial-down :next-form))

(defn spatial-left [editor]
  (apply-move-cursor editor :spatial-left))

(defn spatial-right [editor]
  (apply-move-cursor editor :spatial-right))

; structural movement

(defn structural-up [editor]
  (apply-move-cursor editor :structural-up))

(defn structural-down [editor]
  (apply-move-cursor editor :structural-down))

(defn structural-left [editor]
  (apply-move-cursor editor :structural-left))

(defn structural-right [editor]
  (apply-move-cursor editor :structural-right))

; token movement

(defn next-interest [editor]
  (apply-move-cursor editor :next-interest))

(defn prev-interest [editor]
  (apply-move-cursor editor :prev-interest))

; form movement

(defn prev-form [editor]
  (apply-move-cursor editor :prev-form))

(defn next-form [editor]
  (apply-move-cursor editor :next-form))
