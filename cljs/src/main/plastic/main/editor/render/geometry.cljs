(ns plastic.main.editor.render.geometry
  (:refer-clojure :exclude [find])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.dom.shim]
            [plastic.util.dom :as dom]
            [plastic.onion.api :refer [$]]))

(defn read-geometry [parent-offset dom-node]
  (if-let [node-id (dom/read-node-id dom-node)]
    (let [offset (.offset ($ dom-node))]
      [node-id {:left   (- (.-left offset) (.-left parent-offset))
                :top    (- (.-top offset) (.-top parent-offset))
                :width  (.-offsetWidth dom-node)
                :height (.-offsetHeight dom-node)}])))

(defn collect-geometry [parent-offset dom-nodes]
  (apply hash-map (mapcat (partial read-geometry parent-offset) dom-nodes)))

(defn retrieve-form-nodes-geometries [editor-id form-id node-ids]
  (let [$form ($ (dom/find-plastic-editor-form editor-id form-id))
        form-offset (.offset $form)
        selector (dom/build-nodes-selector node-ids)
        dom-nodes (dom/find-all-as-vec $form selector)]
    (collect-geometry form-offset dom-nodes)))

(defn mid-point [geometry]
  (+ (:left geometry) (/ (:width geometry) 2)))

(defn width [geometry]
  (or (:width geometry) 0))

(defn height [geometry]
  (or (:height geometry) 0))

(defn left-point [geometry]
  (or (:left geometry) 0))

(defn top-point [geometry]
  (or (:top geometry) 0))

(defn right-point [geometry]
  (+ (left-point geometry) (width geometry)))

(defn bottom-point [geometry]
  (+ (top-point geometry) (height geometry)))

(defn overlap-score* [[l1 r1] [l2 r2]]
  {:pre [(<= l1 l2)]}
  (if (< r1 r2)
    (- r1 l2)
    (- r2 l2)))

(defn sanitize-range [[l r]]
  (if (<= (- r l) 0)
    [l (inc l)]
    [l r]))

(defn overlap-score [range1 range2]
  (if (= range1 range2)
    1000000                                                                                                           ; perfect match, regardless of width
    (let [sr1 (sanitize-range range1)
          sr2 (sanitize-range range2)]
      (if (<= (first sr1) (first sr2))
        (overlap-score* sr1 sr2)
        (overlap-score* sr2 sr1)))))
