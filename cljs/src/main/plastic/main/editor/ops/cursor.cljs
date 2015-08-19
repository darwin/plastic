(ns plastic.main.editor.ops.cursor
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch]])
  (:require [plastic.main.editor.model :as editor]
            [plastic.util.helpers :as helpers]
            [plastic.main.editor.render.dom :as dom]))

(defn mid-point [geometry]
  (let [{:keys [left width] :or {left 0 width 0}} geometry]
    (+ left (/ width 2))))

(defn find-best-spatial-match [node-geometry candidates-geometries]
  (let [node-point (mid-point (second node-geometry))
        score (fn [geometry] (helpers/abs (- node-point (mid-point geometry))))
        scores (map (fn [[id geometry]] {:score (score geometry) :id id}) candidates-geometries)
        best-match (first (sort-by :score scores))]
    (:id best-match)))

(defn resolve-best-spatial-match [editor-id form-id node-id candidate-ids]
  (let [node-geometry (first (dom/retrieve-form-nodes-geometries editor-id form-id [node-id]))
        candidates-geometries (dom/retrieve-form-nodes-geometries editor-id form-id candidate-ids)]
    (find-best-spatial-match node-geometry candidates-geometries)))

(defn structural-movemement [editor op]
  (let [cursor-id (editor/get-cursor editor)
        form-id (editor/get-focused-form-id editor)
        structural-web (editor/get-structural-web-for-form editor form-id)]
    (if-let [result-id (op (get structural-web cursor-id))]
      (editor/set-cursor editor result-id))))

(defn find-first-non-empty-line-in-given-direction [spatial-web line direction-fn]
  (first (drop-while #(and (not (nil? %)) (empty? %)) (map spatial-web (iterate direction-fn (direction-fn line))))))

(defn spatial-movement-up-down [editor direction-fn]
  (if-let [cursor-id (editor/get-cursor editor)]
    (let [form-id (editor/get-focused-form-id editor)
          selectables (editor/get-selectables-for-form editor form-id)
          spatial-web (editor/get-spatial-web-for-form editor form-id)
          sel-node (get selectables cursor-id)
          line-selectables (find-first-non-empty-line-in-given-direction spatial-web (:line sel-node) direction-fn)
          line-selections (map :id line-selectables)]
      (if-let [result (resolve-best-spatial-match (editor/get-id editor) form-id (:id sel-node) line-selections)]
        (editor/set-cursor editor result)))
    (let [form-id (or (editor/get-focused-form-id editor) (editor/get-first-form-id editor))
          token-id (editor/get-first-selectable-token-id-for-form editor form-id)]
      (editor/set-cursor editor token-id))))

(defn spatial-movement-left-right [editor direction-fn]
  (let [cursor-id (editor/get-cursor editor)
        form-id (editor/get-focused-form-id editor)
        selectables (editor/get-selectables-for-form editor form-id)
        spatial-web (editor/get-spatial-web-for-form editor form-id)
        sel-node (get selectables cursor-id)]
    (if (empty? (:children sel-node))
      (let [line-selectables (get spatial-web (:line sel-node))]
        (if-let [result (direction-fn #(= (:id %) cursor-id) line-selectables)]
          (editor/set-cursor editor (:id result)))))))

(defn move-to-form [editor direction-fn next-selection-fn]
  (let [focused-form-id (editor/get-focused-form-id editor)
        top-level-ids (editor/get-top-level-form-ids editor)
        next-focused-form-id (direction-fn #(= % focused-form-id) top-level-ids)]
    (if next-focused-form-id
      (let [next-selection (next-selection-fn editor next-focused-form-id)]
        (editor/set-cursor editor next-selection)))))

(defn token-movement-prev-next [editor direction-fn]
  (let [cursor-id (editor/get-cursor editor)
        form-id (editor/get-focused-form-id editor)
        spatial-web (editor/get-spatial-web-for-form editor form-id)
        all-lines (apply concat (vals spatial-web))]
    (if-let [result (direction-fn #(= (:id %) cursor-id) all-lines)]
      (editor/set-cursor editor (:id result)))))

; -------------------------------------------------------------------------------------------------------------------

(defn move-to-spatial-down [editor]
  (spatial-movement-up-down editor inc))

(defn move-to-spatial-up [editor]
  (spatial-movement-up-down editor dec))

(defn move-to-spatial-right [editor]
  (spatial-movement-left-right editor helpers/next-item))

(defn move-to-spatial-left [editor]
  (spatial-movement-left-right editor helpers/prev-item))

(defn move-to-structural-up [editor]
  (structural-movemement editor :up))

(defn move-to-structural-down [editor]
  (structural-movemement editor :down))

(defn move-to-structural-left [editor]
  (structural-movemement editor :left))

(defn move-to-structural-right [editor]
  (structural-movemement editor :right))

(defn move-to-prev-form [editor]
  (move-to-form editor helpers/prev-item editor/get-last-selectable-token-id-for-form))

(defn move-to-next-form [editor]
  (move-to-form editor helpers/next-item editor/get-first-selectable-token-id-for-form))

(defn move-to-next-token [editor]
  (token-movement-prev-next editor helpers/next-item))

(defn move-to-prev-token [editor]
  (token-movement-prev-next editor helpers/prev-item))

; -------------------------------------------------------------------------------------------------------------------

(def movements
  {:spatial-up       move-to-spatial-up
   :spatial-down     move-to-spatial-down
   :spatial-left     move-to-spatial-left
   :spatial-right    move-to-spatial-right
   :structural-up    move-to-structural-up
   :structural-down  move-to-structural-down
   :structural-left  move-to-structural-left
   :structural-right move-to-structural-right
   :prev-form        move-to-prev-form
   :next-form        move-to-next-form
   :prev-token       move-to-prev-token
   :next-token       move-to-next-token})

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

(defn next-token [editor]
  (apply-move-cursor editor :next-token))

(defn prev-token [editor]
  (apply-move-cursor editor :prev-token))

; form movement

(defn prev-form [editor]
  (apply-move-cursor editor :prev-form))

(defn next-form [editor]
  (apply-move-cursor editor :next-form))