(ns plastic.main.frame.subs
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.frame :refer [register-sub]]
            [plastic.util.subs :refer [path-query-factory]]))

; -------------------------------------------------------------------------------------------------------------------

(defn editor-selector [path]
  (fn [editor-id]
    (concat [:editors editor-id] path)))

(defn settings-selector []
  (fn [setting]
    [:settings setting]))

(defn selection-selector []
  (fn [editor-id node-id]
    [:editors editor-id :selection node-id]))

(defn highlight-selector []
  (fn [editor-id node-id]
    [:editors editor-id :highlight node-id]))

(defn editing-selector []
  (fn [editor-id node-id]
    [:editors editor-id :editing node-id]))

(defn layout-unit-node-selector []
  (fn [editor-id unit-id node-id]
    [:editors editor-id :layout unit-id node-id]))

(defn layout-unit-selector []
  (fn [editor-id unit-id]
    [:editors editor-id :layout unit-id]))

(defn analysis-unit-selector []
  (fn [editor-id unit-id]
    [:editors editor-id :analysis unit-id]))

(defn analysis-unit-node-selector []
  (fn [editor-id unit-id node-id]
    [:editors editor-id :analysis unit-id node-id]))

(defn cursor-selector []
  (fn [editor-id node-id]
    [:editors editor-id :cursor node-id]))

(defn focused-unit-selector []
  (fn [editor-id node-id]
    [:editors editor-id :focused-unit-id node-id]))

; -------------------------------------------------------------------------------------------------------------------

(defn define-subscription [context name selector]
  (register-sub context name (path-query-factory context selector)))

(defn register-subs [context]
  (-> context
    (define-subscription :editors [:editors])
    (define-subscription :editor (editor-selector []))
    (define-subscription :editor-uri (editor-selector [:uri]))
    (define-subscription :editor-units (editor-selector [:units]))
    (define-subscription :editor-selection (editor-selector [:selection]))
    (define-subscription :editor-selection-node (selection-selector))
    (define-subscription :editor-highlight (editor-selector [:highlight]))
    (define-subscription :editor-highlight-node (highlight-selector))
    (define-subscription :editor-editing (editor-selector [:editing]))
    (define-subscription :editor-editing-node (editing-selector))
    (define-subscription :editor-layout-unit (layout-unit-selector))
    (define-subscription :editor-layout-unit-node (layout-unit-node-selector))
    (define-subscription :editor-analysis (editor-selector [:analysis]))
    (define-subscription :editor-analysis-unit (analysis-unit-selector))
    (define-subscription :editor-analysis-unit-node (analysis-unit-node-selector))
    (define-subscription :editor-focused-unit-id (editor-selector [:focused-unit-id]))
    (define-subscription :editor-focused-unit-node (focused-unit-selector))
    (define-subscription :editor-cursor (editor-selector [:cursor]))
    (define-subscription :editor-cursor-node (cursor-selector))
    (define-subscription :editor-inline-editor (editor-selector [:inline-editor]))
    (define-subscription :settings (settings-selector))))
