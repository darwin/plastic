(ns plastic.main.editor.ops.movement.spatial
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch]]
                   [plastic.common :refer [process]])
  (:require [plastic.main.editor.model :as editor]
            [plastic.main.editor.render.geometry :as geometry]))

(defn get-left-score-fn [node-geometry]
  (let [node-point (geometry/left-point (second node-geometry))]
    (fn [geometry]
      (let [score (- node-point (geometry/right-point geometry))]
        (if-not (neg? score)
          (- score))))))

(defn get-right-score-fn [node-geometry]
  (let [node-point (geometry/right-point (second node-geometry))]
    (fn [geometry]
      (let [score (- (geometry/left-point geometry) node-point)]
        (if-not (neg? score)
          (- score))))))

(defn get-mid-score-fn [node-geometry]
  (let [node-dim (second node-geometry)
        node-range [(geometry/left-point node-dim) (geometry/right-point node-dim)]]
    (fn [geometry] (geometry/overlap-score node-range [(geometry/left-point geometry)
                                                       (geometry/right-point geometry)]))))

(defn find-best [scores]
  (process scores nil
    (fn [accum item]
      (if (or (nil? (:score accum)) (> (:score item) (:score accum)))
        item
        accum))))

(defn find-best-spatial-match [node-geometry candidates-geometries kind]
  (let [score-fn (case kind
                   :left (get-left-score-fn node-geometry)
                   :right (get-right-score-fn node-geometry)
                   :mid (get-mid-score-fn node-geometry))
        scores (map (fn [geometry] {:score (score-fn geometry) :id (:id geometry)}) candidates-geometries)
        best-match (find-best (remove #(nil? (:score %)) scores))]
    (:id best-match)))

(defn resolve-best-spatial-match [editor-id form-id node-id candidate-ids direction]
  (let [node-geometry (first (geometry/retrieve-form-nodes-geometries editor-id form-id [node-id]))
        candidates-without-node-id (remove #(= % node-id) candidate-ids)
        unsorted-geometries (geometry/retrieve-form-nodes-geometries editor-id form-id candidates-without-node-id)
        candidates-geometries (map #(assoc (get unsorted-geometries %) :id %) candidates-without-node-id)]
    (find-best-spatial-match node-geometry candidates-geometries direction)))

(defn find-first-non-empty-line-in-given-direction [spatial-web line direction-fn]
  (first (drop-while #(and (not (nil? %)) (empty? %)) (map spatial-web (iterate direction-fn (direction-fn line))))))

(defn spatial-movement-up-down [editor direction-fn]
  (if-let [cursor-id (editor/get-cursor editor)]
    (let [form-id (editor/get-focused-form-id editor)
          selectables (editor/get-selectables-for-form editor form-id)
          spatial-web (editor/get-spatial-web-for-form editor form-id)
          sel-node (get selectables cursor-id)
          line-selectables (find-first-non-empty-line-in-given-direction spatial-web (:line sel-node) direction-fn)
          line-ids (map :id line-selectables)]
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
        line-ids (map :id line-selectables)]
    (if-let [result (resolve-best-spatial-match editor-id form-id cursor-id line-ids direction)]
      (editor/set-cursor editor result))))
