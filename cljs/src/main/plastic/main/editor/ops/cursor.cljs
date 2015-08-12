(ns plastic.main.editor.ops.cursor
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main.glue :refer [react! dispatch]])
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

; ----------------------------------------------------------------------------------------------------------------

(defmulti op (fn [op-type & _] op-type))

(defmethod op :spatial-down [_ editor]
  (spatial-movement-up-down editor inc))

(defmethod op :spatial-up [_ editor]
  (spatial-movement-up-down editor dec))

(defmethod op :spatial-right [_ editor]
  (spatial-movement-left-right editor helpers/next-item))

(defmethod op :spatial-left [_ editor]
  (spatial-movement-left-right editor helpers/prev-item))

(defmethod op :structural-up [_ editor]
  (structural-movemement editor :up))

(defmethod op :structural-down [_ editor]
  (structural-movemement editor :down))

(defmethod op :structural-left [_ editor]
  (structural-movemement editor :left))

(defmethod op :structural-right [_ editor]
  (structural-movemement editor :right))

(defmethod op :move-prev-form [_ editor]
  (move-to-form editor helpers/prev-item editor/get-last-selectable-token-id-for-form))

(defmethod op :move-next-form [_ editor]
  (move-to-form editor helpers/next-item editor/get-first-selectable-token-id-for-form))

(defmethod op :next-token [_ editor]
  (token-movement-prev-next editor helpers/next-item))

(defmethod op :prev-token [_ editor]
  (token-movement-prev-next editor helpers/prev-item))

; ----------------------------------------------------------------------------------------------------------------

(defmethod op :default [command]
  (error (str "Unknown selection operation '" command "'"))
  nil)

; ====================================================================================

(defn apply-movements [editor movements]
  (if-let [movement (first movements)]
    (if-let [result (op movement editor)]
      result
      (recur editor (rest movements)))))

(defn apply-move-cursor [editor & movements]
  (if-let [result (apply-movements editor movements)]
    result
    editor))

; ---------------------------
; spatial movement

(defn spatial-up [editor]
  (apply-move-cursor editor :spatial-up :move-prev-form))

(defn spatial-down [editor]
  (apply-move-cursor editor :spatial-down :move-next-form))

(defn spatial-left [editor]
  (apply-move-cursor editor :spatial-left))

(defn spatial-right [editor]
  (apply-move-cursor editor :spatial-right))

; ---------------------------
; structural movement

(defn structural-up [editor]
  (apply-move-cursor editor :structural-up))

(defn structural-down [editor]
  (apply-move-cursor editor :structural-down))

(defn structural-left [editor]
  (apply-move-cursor editor :structural-left))

(defn structural-right [editor]
  (apply-move-cursor editor :structural-right))

; ---------------------------
; token movement

(defn next-token [editor]
  (apply-move-cursor editor :next-token))

(defn prev-token [editor]
  (apply-move-cursor editor :prev-token))

; ---------------------------
; form movement

(defn prev-form [editor]
  (apply-move-cursor editor :move-prev-form))

(defn next-form [editor]
  (apply-move-cursor editor :move-next-form))