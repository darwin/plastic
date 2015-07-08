(ns quark.cogs.editor.selection.model
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]])
  (:require [quark.util.helpers :as helpers]))

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
        _ (log scores)
        best-match (first (sort-by :score scores))]
    (:candidate best-match)))

; ----------------------------------------------------------------------------------------------------------------
(defmulti op (fn [op-type & _] op-type))

(defmethod op :move-down [_ cursel form-info]
  (let [sel-node-id (first cursel)
        {:keys [lines-tokens all-selections]} form-info
        sel-node (get all-selections sel-node-id)
        sel-line (:line sel-node)
        next-line (inc sel-line)
        line-tokens (get lines-tokens next-line)
        line-selections (map #(get all-selections (:id %)) line-tokens)
        result (find-best-spatial-match sel-node line-selections)]
    (if result
      #{(:id result)})))

(defmethod op :move-up [_ cursel form-info]
  (let [sel-node-id (first cursel)
        {:keys [lines-tokens all-selections]} form-info
        sel-node (get all-selections sel-node-id)
        sel-line (:line sel-node)
        next-line (dec sel-line)
        line-tokens (get lines-tokens next-line)
        line-selections (map #(get all-selections (:id %)) line-tokens)
        result (find-best-spatial-match sel-node line-selections)]
    (if result
      #{(:id result)})))

(defmethod op :move-right [_ cursel form-info]
  (let [sel-node-id (first cursel)
        {:keys [lines-tokens all-selections]} form-info
        sel-node (get all-selections sel-node-id)
        sel-line (:line sel-node)
        line-tokens (get lines-tokens sel-line)
        _ (assert line-tokens)
        result (first-node-right line-tokens sel-node-id)]
    (if result
      #{(:id result)})))

(defmethod op :move-left [_ cursel form-info]
  (let [sel-node-id (first cursel)
        {:keys [lines-tokens all-selections]} form-info
        sel-node (get all-selections sel-node-id)
        sel-line (:line sel-node)
        line-tokens (get lines-tokens sel-line)
        _ (assert line-tokens)
        result (first-node-left line-tokens sel-node-id)]
    (if result
      #{(:id result)})))

; ----------------------------------------------------------------------------------------------------------------

(defmethod op :default [command]
  (error (str "Unknown selection operation '" command "'"))
  nil)