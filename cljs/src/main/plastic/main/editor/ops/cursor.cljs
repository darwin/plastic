(ns plastic.main.editor.ops.cursor
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch]]
                   [plastic.common :refer [process]])
  (:require [plastic.main.editor.model :as editor]
            [plastic.util.helpers :as helpers]
            [plastic.main.editor.ops.movement.form :refer [move-form]]
            [plastic.main.editor.ops.movement.interest :refer [interest-movement-prev-next]]
            [plastic.main.editor.ops.movement.spatial :refer [spatial-movement-left-right spatial-movement-up-down]]
            [plastic.main.editor.ops.movement.structural :refer [structural-movemement]]))

; -------------------------------------------------------------------------------------------------------------------

(defn move-spatial-down [editor]
  (spatial-movement-up-down editor inc))

(defn move-spatial-up [editor]
  (spatial-movement-up-down editor dec))

(defn move-spatial-right [editor]
  (spatial-movement-left-right editor :right))

(defn move-spatial-left [editor]
  (spatial-movement-left-right editor :left))

(defn move-structural-up [editor]
  (structural-movemement editor :up))

(defn move-structural-down [editor]
  (structural-movemement editor :down))

(defn move-structural-left [editor]
  (structural-movemement editor :left))

(defn move-structural-right [editor]
  (structural-movemement editor :right))

(defn move-prev-form [editor]
  (move-form editor helpers/prev-item editor/get-last-selectable-token-id-for-form))

(defn move-next-form [editor]
  (move-form editor helpers/next-item editor/get-first-selectable-token-id-for-form))

(defn move-next-interest [editor]
  (interest-movement-prev-next editor helpers/next-item))

(defn move-prev-interest [editor]
  (interest-movement-prev-next editor helpers/prev-item))

; -------------------------------------------------------------------------------------------------------------------

(def movements
  {:spatial-up       move-spatial-up
   :spatial-down     move-spatial-down
   :spatial-left     move-spatial-left
   :spatial-right    move-spatial-right
   :structural-up    move-structural-up
   :structural-down  move-structural-down
   :structural-left  move-structural-left
   :structural-right move-structural-right
   :prev-form        move-prev-form
   :next-form        move-next-form
   :prev-interest    move-prev-interest
   :next-interest    move-next-interest})

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
