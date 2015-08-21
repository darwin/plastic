(ns plastic.main.editor.ops.cursor
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch]])
  (:require [plastic.main.editor.model :as editor]
            [plastic.util.helpers :as helpers]
            [plastic.main.editor.render.geometry :as geometry]))

(defn get-left-score-fn [node-geometry]
  (let [node-point (geometry/left-point (second node-geometry))]
    (fn [geometry] (- node-point (geometry/right-point geometry)))))

(defn get-right-score-fn [node-geometry]
  (let [node-point (geometry/right-point (second node-geometry))]
    (fn [geometry] (- (geometry/left-point geometry) node-point))))

(defn get-mid-score-fn [node-geometry]
  (let [node-dim (second node-geometry)
        node-range [(geometry/left-point node-dim) (geometry/right-point node-dim)]]
    (fn [geometry] (geometry/overlap node-range [(geometry/left-point geometry) (geometry/right-point geometry)]))))

(defn find-best-spatial-match [node-geometry candidates-geometries kind]
  (let [score-fn (case kind
                   :left (get-left-score-fn node-geometry)
                   :right (get-right-score-fn node-geometry)
                   :mid (get-mid-score-fn node-geometry))
        best (if (= kind :mid) last first)
        scores (map (fn [[id geometry]] {:score (score-fn geometry) :id id}) candidates-geometries)
        best-match (best (sort-by :score (remove #(neg? (:score %)) scores)))]
    (:id best-match)))

(defn resolve-best-spatial-match [editor-id form-id node-id candidate-ids direction]
  (let [node-geometry (first (geometry/retrieve-form-nodes-geometries editor-id form-id [node-id]))
        candidates-without-node-id (disj candidate-ids node-id)
        candidates-geometries (geometry/retrieve-form-nodes-geometries editor-id form-id candidates-without-node-id)]
    (find-best-spatial-match node-geometry candidates-geometries direction)))

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
          line-ids (set (map :id line-selectables))]
      (if-let [result (resolve-best-spatial-match (editor/get-id editor) form-id (:id sel-node) line-ids :mid)]
        (editor/set-cursor editor result)))
    (let [form-id (or (editor/get-focused-form-id editor) (editor/get-first-form-id editor))
          token-id (editor/get-first-selectable-token-id-for-form editor form-id)]
      (editor/set-cursor editor token-id))))

(defn spatial-movement-left-right [editor direction]
  (let [editor-id (editor/get-id editor)
        cursor-id (editor/get-cursor editor)
        form-id (editor/get-focused-form-id editor)
        selectables (editor/get-selectables-for-form editor form-id)
        spatial-web (editor/get-spatial-web-for-form editor form-id)
        sel-node (get selectables cursor-id)
        line-selectables (get spatial-web (:line sel-node))
        line-ids (set (map :id line-selectables))]
    (if-let [result (resolve-best-spatial-match editor-id form-id cursor-id line-ids direction)]
      (editor/set-cursor editor result))))

(defn move-form [editor direction-fn next-selection-fn]
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

(defn move-next-token [editor]
  (token-movement-prev-next editor helpers/next-item))

(defn move-prev-token [editor]
  (token-movement-prev-next editor helpers/prev-item))

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
   :prev-token       move-prev-token
   :next-token       move-next-token})

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
