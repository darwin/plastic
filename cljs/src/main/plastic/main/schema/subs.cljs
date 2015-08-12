(ns plastic.main.schema.subs
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main.glue :refer [react!]]
                   [reagent.ratom :refer [reaction]])
  (:require [plastic.main.frame.core :refer [register-sub]]
            [plastic.main.schema.paths :as paths]
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

(defn editing-selector []
  (fn [editor-id node-id]
    (concat paths/editors [editor-id :editing node-id])))

(defn form-node-layout-selector []
  (fn [editor-id form-id node-id]
    (concat paths/editors [editor-id :layout form-id node-id])))

(defn form-layout-selector []
  (fn [editor-id form-id]
    (concat paths/editors [editor-id :layout form-id])))

(defn analysis-selector []
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
(register-sub :editor-uri (path-query-factory (editor-selector [:def :uri])))
(register-sub :editor-text (path-query-factory (editor-selector [:text])))
(register-sub :editor-parse-tree (path-query-factory (editor-selector [:parse-tree])))
(register-sub :editor-cursors (path-query-factory (editor-selector [:cursors])))
(register-sub :editor-selection (path-query-factory (editor-selector [:selection])))
(register-sub :editor-selection-node (path-query-factory (selection-selector)))
(register-sub :editor-editing (path-query-factory (editor-selector [:editing])))
(register-sub :editor-editing-node (path-query-factory (editing-selector)))
(register-sub :editor-form-layout (path-query-factory (form-layout-selector)))
(register-sub :editor-form-node-layout (path-query-factory (form-node-layout-selector)))
(register-sub :editor-form-node-analysis (path-query-factory (analysis-selector)))
(register-sub :editor-focused-form-id (path-query-factory (editor-selector [:focused-form-id])))
(register-sub :editor-focused-form-node (path-query-factory (focused-form-selector)))
(register-sub :editor-cursor (path-query-factory (editor-selector [:cursor])))
(register-sub :editor-cursor-node (path-query-factory (cursor-selector)))

(register-sub :settings (path-query-factory (settings-selector)))