(ns plastic.main.subs
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react!]]
                   [reagent.ratom :refer [reaction]])
  (:require [plastic.main.frame :refer [register-sub]]
            [plastic.main.paths :as paths]
            [plastic.util.subs :refer [path-query-factory]]))

(defn editor-selector [path]
  (fn [editor-id]
    (concat paths/editors [editor-id] path)))

(defn settings-selector []
  (fn [setting]
    (concat paths/settings [setting])))

(defn selection-selector []
  (fn [editor-id node-id]
    (concat paths/editors [editor-id :selection node-id])))

(defn highlight-selector []
  (fn [editor-id node-id]
    (concat paths/editors [editor-id :highlight node-id])))

(defn editing-selector []
  (fn [editor-id node-id]
    (concat paths/editors [editor-id :editing node-id])))

(defn layout-unit-node-selector []
  (fn [editor-id unit-id node-id]
    (concat paths/editors [editor-id :layout unit-id node-id])))

(defn layout-unit-selector []
  (fn [editor-id unit-id]
    (concat paths/editors [editor-id :layout unit-id])))

(defn analysis-unit-selector []
  (fn [editor-id unit-id]
    (concat paths/editors [editor-id :analysis unit-id])))

(defn analysis-unit-node-selector []
  (fn [editor-id unit-id node-id]
    (concat paths/editors [editor-id :analysis unit-id node-id])))

(defn cursor-selector []
  (fn [editor-id node-id]
    (concat paths/editors [editor-id :cursor node-id])))

(defn focused-unit-selector []
  (fn [editor-id node-id]
    (concat paths/editors [editor-id :focused-unit-id node-id])))

(register-sub :editors (path-query-factory paths/editors))

(register-sub :editor-uri (path-query-factory (editor-selector [:uri])))
(register-sub :editor-units (path-query-factory (editor-selector [:units])))
(register-sub :editor-selection (path-query-factory (editor-selector [:selection])))
(register-sub :editor-selection-node (path-query-factory (selection-selector)))
(register-sub :editor-highlight (path-query-factory (editor-selector [:highlight])))
(register-sub :editor-highlight-node (path-query-factory (highlight-selector)))
(register-sub :editor-editing (path-query-factory (editor-selector [:editing])))
(register-sub :editor-editing-node (path-query-factory (editing-selector)))
(register-sub :editor-layout-unit (path-query-factory (layout-unit-selector)))
(register-sub :editor-layout-unit-node (path-query-factory (layout-unit-node-selector)))
(register-sub :editor-analysis (path-query-factory (editor-selector [:analysis])))
(register-sub :editor-analysis-unit (path-query-factory (analysis-unit-selector)))
(register-sub :editor-analysis-unit-node (path-query-factory (analysis-unit-node-selector)))
(register-sub :editor-focused-unit-id (path-query-factory (editor-selector [:focused-unit-id])))
(register-sub :editor-focused-unit-node (path-query-factory (focused-unit-selector)))
(register-sub :editor-cursor (path-query-factory (editor-selector [:cursor])))
(register-sub :editor-cursor-node (path-query-factory (cursor-selector)))
(register-sub :editor-inline-editor (path-query-factory (editor-selector [:inline-editor])))

(register-sub :settings (path-query-factory (settings-selector)))
