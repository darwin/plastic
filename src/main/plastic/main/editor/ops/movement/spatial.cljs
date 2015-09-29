(ns plastic.main.editor.ops.movement.spatial
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.common :refer [process]])
  (:require [plastic.main.editor.model :as editor]
            [plastic.main.editor.render.geometry :as geometry]
            [plastic.util.helpers :as helpers]))

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

(defn get-horizontal-score-fn [node-geometry]
  (let [node-dim (second node-geometry)
        node-range [(geometry/left-point node-dim) (geometry/right-point node-dim)]]
    (fn [geometry]
      (let [geometry-range [(geometry/left-point geometry) (geometry/right-point geometry)]]
        (geometry/overlap-score node-range geometry-range)))))

(defn get-vertical-score-fn [node-geometry]
  (let [node-dim (second node-geometry)
        node-range [(geometry/top-point node-dim) (geometry/bottom-point node-dim)]]
    (fn [geometry]
      (let [geometry-range [(geometry/top-point geometry) (geometry/bottom-point geometry)]]
        (geometry/overlap-score node-range geometry-range)))))

(defn find-best [scores]
  (process scores nil
    (fn [accum item]
      (if (or (nil? (:score accum)) (> (:score item) (:score accum)))
        item
        accum))))

(defn find-best-spatial-match [node-geometry candidates-geometries kind]
  (let [score-fn (case kind
                   (:up :down) (get-horizontal-score-fn node-geometry)
                   :vertical (get-vertical-score-fn node-geometry)
                   :left (get-left-score-fn node-geometry)
                   :right (get-right-score-fn node-geometry))
        scores (map (fn [geometry] {:score (score-fn geometry) :id (:id geometry)}) candidates-geometries)
        best-match (find-best (remove #(nil? (:score %)) scores))]
    (:id best-match)))

(defn resolve-best-spatial-match [editor-id form-id node-id candidate-ids direction]
  (let [node-geometry (first (geometry/retrieve-unit-nodes-geometries editor-id form-id [node-id]))
        candidates-without-node-id (remove #(= % node-id) candidate-ids)
        unsorted-geometries (geometry/retrieve-unit-nodes-geometries editor-id form-id candidates-without-node-id)
        candidates-geometries (map #(assoc (get unsorted-geometries %) :id %) candidates-without-node-id)]
    (find-best-spatial-match node-geometry candidates-geometries direction)))

(defn find-next-spatial-group [spatial-graph spatial-index direction]
  (let [advance (case direction
                  :up dec
                  :down inc)
        next-spatial-index (advance spatial-index)]
    (get spatial-graph next-spatial-index)))

(defn find-spatial-graph [editor form-id section]
  {:pre [(#{:headers :docs :code :comments} section)]}
  (let [spatial-web (editor/get-spatial-web-for-unit editor form-id)]
    (section spatial-web)))

;
; prev-form
;  |
; docs?
;  |    \
; code? - comments?
;  |    /
; next-form
;
(def section-graph
  {:docs     {:up   :up
              :down :code}
   :code     {:up    :docs
              :down  :down
              :right :comments}
   :comments {:up   :docs
              :down :down
              :left :code}})

(defn get-graph-column [selector spatial-graph]
  (remove nil? (map selector (filter vector? (vals spatial-graph)))))

(defn enter-spatial-group [editor spatial-group form-id node-id direction]
  (let [editor-id (editor/get-id editor)
        candidate-ids (map :id spatial-group)
        match-direction (if (#{:left :right} direction) :vertical direction)
        result (if node-id
                 (resolve-best-spatial-match editor-id form-id node-id candidate-ids match-direction)
                 (if (= direction :up) (last candidate-ids) (first candidate-ids)))]
    (if result
      (editor/set-cursor editor result))))

(defn enter-spatial-graph [editor spatial-graph form-id node-id direction]
  (let [spatial-group (case direction
                        :up (get spatial-graph (:max spatial-graph))
                        :down (get spatial-graph (:min spatial-graph))
                        :left (get-graph-column last spatial-graph)
                        :right (get-graph-column first spatial-graph))]
    (enter-spatial-group editor spatial-group form-id node-id direction)))

(defn get-next-form-id [editor form-id direction]
  (let [top-level-ids (editor/get-units editor)
        move (case direction
               :down helpers/next-item
               :up helpers/prev-item)]
    (move #(= % form-id) top-level-ids)))

(defn get-spatial-group [editor form-id min-or-max]
  (let [docs-spatial-graph (find-spatial-graph editor form-id :docs)
        code-spatial-graph (find-spatial-graph editor form-id :code)
        comments-spatial-graph (find-spatial-graph editor form-id :comments)
        code+comments-group (concat
                              (get code-spatial-graph (min-or-max code-spatial-graph))
                              (get comments-spatial-graph (min-or-max comments-spatial-graph)))

        docs-group (get docs-spatial-graph (min-or-max docs-spatial-graph))]
    (case min-or-max
      :min (or docs-group code+comments-group)
      :max (or code+comments-group docs-group))))

(defn spatial-form-movement [editor form-id node-id direction]
  (if-let [next-form-id (get-next-form-id editor form-id direction)]
    (let [spatial-group (case direction
                          :up (get-spatial-group editor next-form-id :max)
                          :down (get-spatial-group editor next-form-id :min))]
      (enter-spatial-group editor spatial-group next-form-id node-id direction))))

(defn spatial-section-movement [editor form-id node-id direction]
  (let [selectables (editor/get-selectables-for-unit editor form-id)
        selectable (get selectables node-id)]
    (loop [section (:section selectable)]
      (let [next-section (get-in section-graph [section direction])]
        (case next-section
          (:up :down) (spatial-form-movement editor form-id node-id next-section)
          (:docs :code :comments) (if-let [spatial-graph (find-spatial-graph editor form-id next-section)]
                                    (enter-spatial-graph editor spatial-graph form-id node-id direction)
                                    (recur next-section))
          nil)))))

(defn spatial-token-movement [editor form-id node-id direction]
  (let [editor-id (editor/get-id editor)
        selectables (editor/get-selectables-for-unit editor form-id)]
    (if-let [selectable (get selectables node-id)]
      (let [spatial-graph (find-spatial-graph editor form-id (:section selectable))
            spatial-group (case direction
                            (:left :right) (get spatial-graph (:spatial-index selectable))
                            (:up :down) (find-next-spatial-group spatial-graph (:spatial-index selectable) direction))
            candidate-ids (map :id spatial-group)]
        (if-let [result (resolve-best-spatial-match editor-id form-id node-id candidate-ids direction)]
          (editor/set-cursor editor result))))))

(defn spatial-movement [direction editor]
  (let [form-id (editor/get-focused-unit-id editor)]
    (if-let [cursor-id (editor/get-cursor editor)]
      (or
        (spatial-token-movement editor form-id cursor-id direction)
        (spatial-section-movement editor form-id cursor-id direction)))))

