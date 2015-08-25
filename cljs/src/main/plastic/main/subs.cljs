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

(defn layout-form-node-selector []
  (fn [editor-id form-id node-id]
    (concat paths/editors [editor-id :layout form-id node-id])))

(defn layout-form-selector []
  (fn [editor-id form-id]
    (concat paths/editors [editor-id :layout form-id])))

(defn analysis-form-selector []
  (fn [editor-id form-id]
    (concat paths/editors [editor-id :analysis form-id])))

(defn analysis-form-node-selector []
  (fn [editor-id form-id node-id]
    (concat paths/editors [editor-id :analysis form-id node-id])))

(defn cursor-selector []
  (fn [editor-id node-id]
    (concat paths/editors [editor-id :cursor node-id])))

(defn focused-form-selector []
  (fn [editor-id node-id]
    (concat paths/editors [editor-id :focused-form-id node-id])))

(register-sub :editors (path-query-factory paths/editors))

(register-sub :editor-render-state (path-query-factory (editor-selector [:render-state])))
(register-sub :editor-uri (path-query-factory (editor-selector [:uri])))
(register-sub :editor-selection (path-query-factory (editor-selector [:selection])))
(register-sub :editor-selection-node (path-query-factory (selection-selector)))
(register-sub :editor-highlight (path-query-factory (editor-selector [:highlight])))
(register-sub :editor-highlight-node (path-query-factory (highlight-selector)))
(register-sub :editor-editing (path-query-factory (editor-selector [:editing])))
(register-sub :editor-editing-node (path-query-factory (editing-selector)))
(register-sub :editor-layout-form (path-query-factory (layout-form-selector)))
(register-sub :editor-layout-form-node (path-query-factory (layout-form-node-selector)))
(register-sub :editor-analysis (path-query-factory (editor-selector [:analysis])))
(register-sub :editor-analysis-form (path-query-factory (analysis-form-selector)))
(register-sub :editor-analysis-form-node (path-query-factory (analysis-form-node-selector)))
(register-sub :editor-focused-form-id (path-query-factory (editor-selector [:focused-form-id])))
(register-sub :editor-focused-form-node (path-query-factory (focused-form-selector)))
(register-sub :editor-cursor (path-query-factory (editor-selector [:cursor])))
(register-sub :editor-cursor-node (path-query-factory (cursor-selector)))
(register-sub :editor-inline-editor (path-query-factory (editor-selector [:inline-editor])))

(register-sub :settings (path-query-factory (settings-selector)))
