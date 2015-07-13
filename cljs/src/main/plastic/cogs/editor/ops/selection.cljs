(ns plastic.cogs.editor.ops.selection
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]]
                   [plastic.macros.common :refer [*->]])
  (:require [plastic.cogs.editor.layout.utils :refer [apply-to-selected-editors]]
            [plastic.cogs.editor.model :as editor]
            [plastic.util.helpers :as helpers]))

(defn mid-point [node]
  (let [{:keys [left width] :or {left 0 width 0}} (:geometry node)]
    (+ left (/ width 2))))

(defn find-best-spatial-match [node candidates]
  (let [node-point (mid-point node)
        score (fn [candidate] (helpers/abs (- node-point (mid-point candidate))))
        scores (map (fn [candidate] {:score (score candidate) :candidate candidate}) candidates)
        best-match (first (sort-by :score scores))]
    (:candidate best-match)))

(defn get-selected-node-id [editor]
  (first (editor/get-selection editor)))

(defn structural-movemement [editor op]
  (let [selected-id (get-selected-node-id editor)
        render-info (editor/get-focused-render-info editor)
        structural-web (:structural-web render-info)]
    (if-let [result-id (op (get structural-web selected-id))]
      (editor/set-selection editor #{result-id}))))

; ----------------------------------------------------------------------------------------------------------------

(defmulti op (fn [op-type & _] op-type))

(defmethod op :spatial-down [_ editor]
  (let [selected-id (get-selected-node-id editor)
        render-info (editor/get-focused-render-info editor)
        {:keys [spatial-web all-selections]} render-info
        sel-node (get all-selections selected-id)
        sel-line (:line sel-node)
        next-line (inc sel-line)
        line-selectables (get spatial-web next-line)
        line-selections (map #(get all-selections (:id %)) line-selectables)
        result (find-best-spatial-match sel-node line-selections)]
    (if result
      (editor/set-selection editor #{(:id result)}))))

(defmethod op :spatial-up [_ editor]
  (let [selected-id (get-selected-node-id editor)
        render-info (editor/get-focused-render-info editor)
        {:keys [spatial-web all-selections]} render-info
        sel-node (get all-selections selected-id)
        sel-line (:line sel-node)
        next-line (dec sel-line)
        line-selectables (get spatial-web next-line)
        line-selections (map #(get all-selections (:id %)) line-selectables)
        result (find-best-spatial-match sel-node line-selections)]
    (if result
      (editor/set-selection editor #{(:id result)}))))

(defmethod op :spatial-right [_ editor]
  (let [selected-id (get-selected-node-id editor)
        render-info (editor/get-focused-render-info editor)
        {:keys [spatial-web all-selections]} render-info
        sel-node (get all-selections selected-id)
        sel-line (:line sel-node)
        line-selectables (get spatial-web sel-line)
        _ (assert line-selectables)
        result (helpers/next-item #(= (:id %) selected-id) line-selectables)]
    (if result
      (editor/set-selection editor #{(:id result)}))))

(defmethod op :spatial-left [_ editor]
  (let [selected-id (get-selected-node-id editor)
        render-info (editor/get-focused-render-info editor)
        {:keys [spatial-web all-selections]} render-info
        sel-node (get all-selections selected-id)
        sel-line (:line sel-node)
        line-selectables (get spatial-web sel-line)
        _ (assert line-selectables)
        result (helpers/prev-item #(= (:id %) selected-id) line-selectables)]
    (if result
      (editor/set-selection editor #{(:id result)}))))

(defmethod op :structural-up [_ editor]
  (structural-movemement editor :up))

(defmethod op :structural-down [_ editor]
  (structural-movemement editor :down))

(defmethod op :structural-left [_ editor]
  (structural-movemement editor :left))

(defmethod op :structural-right [_ editor]
  (structural-movemement editor :right))

(defmethod op :move-prev-form [_ editor]
  (let [focused-form-id (editor/get-focused-form-id editor)
        top-level-ids (editor/get-top-level-form-ids editor)
        next-focused-form-id (helpers/prev-item #(= % focused-form-id) top-level-ids)]
    (if next-focused-form-id
      (let [next-selection (editor/get-last-selectable-token-id-for-form editor next-focused-form-id)]
        (-> editor
          (editor/set-selection #{})
          (editor/set-focused-form-id next-focused-form-id)
          (editor/set-selection #{next-selection}))))))

(defmethod op :move-next-form [_ editor]
  (let [focused-form-id (editor/get-focused-form-id editor)
        top-level-ids (editor/get-top-level-form-ids editor)
        next-focused-form-id (helpers/next-item #(= % focused-form-id) top-level-ids)]
    (if next-focused-form-id
      (let [next-selection (editor/get-first-selectable-token-id-for-form editor next-focused-form-id)]
        (-> editor
          (editor/set-selection #{})
          (editor/set-focused-form-id next-focused-form-id)
          (editor/set-selection #{next-selection}))))))

; ----------------------------------------------------------------------------------------------------------------

(defmethod op :default [command]
  (error (str "Unknown selection operation '" command "'"))
  nil)

; ====================================================================================

; editor's :selections is a map of form-ids to sets of selected node-ids
; also has key :focused-form-id pointing to currently focused form

(defn apply-movements [editor movements]
  (if-let [movement (first movements)]
    (if-let [result (op movement editor)]
      result
      (recur editor (rest movements)))))

(defn apply-move-selection [editor & movements]
  (if-let [result (apply-movements editor movements)]
    result
    editor))

; ---------------------------
; spatial movement

(defn spatial-up [editor]
  (apply-move-selection editor :spatial-up :move-prev-form))

(defn spatial-down [editor]
  (apply-move-selection editor :spatial-down :move-next-form))

(defn spatial-left [editor]
  (apply-move-selection editor :spatial-left))

(defn spatial-right [editor]
  (apply-move-selection editor :spatial-right))

; ---------------------------
; structural movement

(defn structural-up [editor]
  (apply-move-selection editor :structural-up))

(defn structural-down [editor]
  (apply-move-selection editor :structural-down))

(defn structural-left [editor]
  (apply-move-selection editor :structural-left))

(defn structural-right [editor]
  (apply-move-selection editor :structural-right))