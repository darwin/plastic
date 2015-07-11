(ns plastic.cogs.editor.selection.model
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.helpers :as helpers]
            [plastic.cogs.editor.model :as editor]))

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
  (first (editor/get-focused-selection editor)))

; ----------------------------------------------------------------------------------------------------------------
(defmulti op (fn [op-type & _] op-type))

(defmethod op :move-down [_ editor]
  (let [selected-id (get-selected-node-id editor)
        render-info (editor/get-focused-render-info editor)
        {:keys [lines-selectables all-selections]} render-info
        sel-node (get all-selections selected-id)
        sel-line (:line sel-node)
        next-line (inc sel-line)
        line-selectables (get lines-selectables next-line)
        line-selections (map #(get all-selections (:id %)) line-selectables)
        result (find-best-spatial-match sel-node line-selections)]
    (if result
      (editor/set-focused-selection editor #{(:id result)}))))

(defmethod op :move-up [_ editor]
  (let [selected-id (get-selected-node-id editor)
        render-info (editor/get-focused-render-info editor)
        {:keys [lines-selectables all-selections]} render-info
        sel-node (get all-selections selected-id)
        sel-line (:line sel-node)
        next-line (dec sel-line)
        line-selectables (get lines-selectables next-line)
        line-selections (map #(get all-selections (:id %)) line-selectables)
        result (find-best-spatial-match sel-node line-selections)]
    (if result
      (editor/set-focused-selection editor #{(:id result)}))))

(defmethod op :move-right [_ editor]
  (let [selected-id (get-selected-node-id editor)
        render-info (editor/get-focused-render-info editor)
        {:keys [lines-selectables all-selections]} render-info
        sel-node (get all-selections selected-id)
        sel-line (:line sel-node)
        line-selectables (get lines-selectables sel-line)
        _ (assert line-selectables)
        result (helpers/next-item #(= (:id %) selected-id) line-selectables)]
    (if result
      (editor/set-focused-selection editor #{(:id result)}))))

(defmethod op :move-left [_ editor]
  (let [selected-id (get-selected-node-id editor)
        render-info (editor/get-focused-render-info editor)
        {:keys [lines-selectables all-selections]} render-info
        sel-node (get all-selections selected-id)
        sel-line (:line sel-node)
        line-selectables (get lines-selectables sel-line)
        _ (assert line-selectables)
        result (helpers/prev-item #(= (:id %) selected-id) line-selectables)]
    (if result
      (editor/set-focused-selection editor #{(:id result)}))))

(defmethod op :level-up [_ editor]
  (let [selected-id (get-selected-node-id editor)
        render-info (editor/get-focused-render-info editor)
        {:keys [selectable-parents]} render-info
        result-id (get selectable-parents selected-id)]
    (if result-id
      (editor/set-focused-selection editor #{result-id}))))

(defmethod op :level-down [_ editor]
  (let [selected-id (get-selected-node-id editor)
        render-info (editor/get-focused-render-info editor)
        {:keys [selectable-parents]} render-info
        children-ids-having-sel-node-as-parent (map first (filter (fn [[_nid pid]] (= pid selected-id)) selectable-parents))
        candidates (sort children-ids-having-sel-node-as-parent)
        result-id (first candidates)]
    (if result-id
      (editor/set-focused-selection editor #{result-id}))))

(defmethod op :move-prev-form [_ editor]
  (let [focused-form-id (editor/get-focused-form-id editor)
        top-level-ids (editor/get-top-level-form-ids editor)
        next-focused-form-id (helpers/prev-item #(= % focused-form-id) top-level-ids)]
    (if next-focused-form-id
      (let [next-selection (editor/get-last-selectable-id-for-form editor next-focused-form-id)]
        (-> editor
          (editor/set-focused-selection #{})
          (editor/set-focused-form-id next-focused-form-id)
          (editor/set-focused-selection #{next-selection}))))))

(defmethod op :move-next-form [_ editor]
  (let [focused-form-id (editor/get-focused-form-id editor)
        top-level-ids (editor/get-top-level-form-ids editor)
        next-focused-form-id (helpers/next-item #(= % focused-form-id) top-level-ids)]
    (if next-focused-form-id
      (let [next-selection (editor/get-first-selectable-id-for-form editor next-focused-form-id)]
        (-> editor
          (editor/set-focused-selection #{})
          (editor/set-focused-form-id next-focused-form-id)
          (editor/set-focused-selection #{next-selection}))))))

; ----------------------------------------------------------------------------------------------------------------

(defmethod op :default [command]
  (error (str "Unknown selection operation '" command "'"))
  nil)