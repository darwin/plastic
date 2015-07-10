(ns plastic.cogs.editor.selection.model
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.helpers :as helpers]))

(defn first-node-right [nodes id]
  (nth (drop-while #(not= (:id %) id) nodes) 1 nil))

(defn first-node-left [nodes id]
  (last (take-while #(not= (:id %) id) nodes)))

(defn mid-point [node]
  (let [{:keys [left width] :or {left 0 width 0}} (:geometry node)]
    (+ left (/ width 2))))

(defn find-best-spatial-match [node candidates]
  (let [node-point (mid-point node)
        score (fn [candidate] (helpers/abs (- node-point (mid-point candidate))))
        scores (map (fn [candidate] {:score (score candidate) :candidate candidate}) candidates)
        best-match (first (sort-by :score scores))]
    (:candidate best-match)))

; ----------------------------------------------------------------------------------------------------------------
(defmulti op (fn [op-type & _] op-type))

(defmethod op :move-down [_ cursel form-info]
  (let [sel-node-id (first cursel)
        {:keys [lines-selectables all-selections]} form-info
        sel-node (get all-selections sel-node-id)
        sel-line (:line sel-node)
        next-line (inc sel-line)
        line-selectables (get lines-selectables next-line)
        line-selections (map #(get all-selections (:id %)) line-selectables)
        result (find-best-spatial-match sel-node line-selections)]
    (if result
      #{(:id result)})))

(defmethod op :move-up [_ cursel form-info]
  (let [sel-node-id (first cursel)
        {:keys [lines-selectables all-selections]} form-info
        sel-node (get all-selections sel-node-id)
        sel-line (:line sel-node)
        next-line (dec sel-line)
        line-selectables (get lines-selectables next-line)
        line-selections (map #(get all-selections (:id %)) line-selectables)
        result (find-best-spatial-match sel-node line-selections)]
    (if result
      #{(:id result)})))

(defmethod op :move-right [_ cursel form-info]
  (let [sel-node-id (first cursel)
        {:keys [lines-selectables all-selections]} form-info
        sel-node (get all-selections sel-node-id)
        sel-line (:line sel-node)
        line-selectables (get lines-selectables sel-line)
        _ (assert line-selectables)
        result (first-node-right line-selectables sel-node-id)]
    (if result
      #{(:id result)})))

(defmethod op :move-left [_ cursel form-info]
  (let [sel-node-id (first cursel)
        {:keys [lines-selectables all-selections]} form-info
        sel-node (get all-selections sel-node-id)
        sel-line (:line sel-node)
        line-selectables (get lines-selectables sel-line)
        _ (assert line-selectables)
        result (first-node-left line-selectables sel-node-id)]
    (if result
      #{(:id result)})))

(defmethod op :level-up [_ cursel form-info]
  (let [sel-node-id (first cursel)
        {:keys [selectable-parents]} form-info
        result (get selectable-parents sel-node-id)]
    (if result
      #{result})))

(defmethod op :level-down [_ cursel form-info]
  (let [sel-node-id (first cursel)
        {:keys [selectable-parents]} form-info
        candidates (sort (map first (filter (fn [[_nid pid]] (= pid sel-node-id)) selectable-parents)))
        result (first candidates)]
    (if result
      #{result})))

; ----------------------------------------------------------------------------------------------------------------

(defmethod op :default [command]
  (error (str "Unknown selection operation '" command "'"))
  nil)