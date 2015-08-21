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
