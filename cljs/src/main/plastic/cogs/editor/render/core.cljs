(ns plastic.cogs.editor.render.core
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]]
                   [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [plastic.frame.core :refer [register-sub subscribe]]
            [plastic.cogs.editor.render.headers :refer [headers-group-component]]
            [plastic.cogs.editor.render.docs :refer [docs-group-component]]
            [plastic.cogs.editor.render.code :refer [code-box-component]]
            [plastic.cogs.editor.render.debug :refer [parser-debug-component text-input-debug-component text-output-debug-component render-tree-debug-component selections-debug-overlay-component]]
            [plastic.cogs.editor.render.utils :refer [dangerously-set-html classv]]
            [plastic.cogs.editor.render.dom :as dom]))

(declare unified-rendering-component)

(defn render-tree-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-form-node-layout editor-id form-id node-id])
        selection-subscription (subscribe [:editor-selection-node editor-id node-id])]
    (fn [editor-id form-id node-id]
      (log "R! render-tree" node-id)
      (let [{:keys [id tag children selectable?]} @layout]
        [:div {:data-qnid id
               :class     (classv
                            (name tag)
                            (if selectable? "selectable")
                            (if (and selectable? @selection-subscription) "selected"))}
         (for [child-id children]
           ^{:key child-id} [unified-rendering-component editor-id form-id child-id])]))))

(defn unified-rendering-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-form-node-layout editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (let [{:keys [id tag]} @layout]
        (log "R! unified-rendering" form-id node-id id tag @layout)
        (if id
          [:div.unified {:data-qnid id}
           (condp = tag
             :tree [render-tree-component editor-id form-id id node-id]
             :code [code-box-component editor-id form-id node-id]
             :docs [docs-group-component editor-id form-id node-id]
             :headers [headers-group-component editor-id form-id node-id]
             (throw (str "don't know how to render tag " tag " (missing render component implementation)")))]
          [:div])))))

(defn form-skelet-component []
  (fn [editor-id form-id]
    (log "R! form-skelet" form-id)
    [:div.form-skelet
     [unified-rendering-component editor-id form-id :root]]))

(defn handle-form-click [form-id event]
  (let [target-dom-node (.-target event)
        _ (assert target-dom-node)
        selectable-dom-node (dom/find-closest target-dom-node ".selectable")]
    (if selectable-dom-node
      (let [selected-node-id (dom/read-node-id selectable-dom-node)
            _ (assert selected-node-id)
            editor-id (dom/lookup-editor-id selectable-dom-node)]
        (.stopPropagation event)
        (dispatch :editor-focus-form editor-id form-id)
        (if-not (dom/event-shift-key? event)
          (do
            (dispatch :editor-set-selection editor-id #{selected-node-id})
            (dispatch :editor-set-cursor editor-id selected-node-id))
          (dispatch :editor-toggle-selection editor-id #{selected-node-id}))))))

(defn form-component [editor-id form-id]
  (let [focused-form-id (subscribe [:editor-focused-form-id editor-id])]
  (fn [editor-id form-id]
    (let [focused? (= form-id @focused-form-id)]
      (log "R! form" form-id "focused" focused?)
      [:tr
       [:td
        [:div.form
         {:data-qnid form-id
          :class     (if focused? "focused")
          :on-click  (partial handle-form-click form-id)}
         [form-skelet-component editor-id form-id]]]]))))

(defn forms-component [editor-id order]
  (fn [editor-id order]
    (log "R! forms" editor-id)
    [:table.form-table
     [:tbody
      (for [form-id order]
        ^{:key form-id} [form-component editor-id form-id])]]))

(defn handle-editor-click [editor-id event]
  (.stopPropagation event)
  (dispatch :editor-clear-selection editor-id)
  (dispatch :editor-clear-cursor editor-id))

(def ^:dynamic last-st :nil)

(defn editor-root-component [editor-id]
  (let [state (subscribe [:editor-render-state editor-id])
        parser-debug-visible (subscribe [:settings :parser-debug-visible])
        text-input-debug-visible (subscribe [:settings :text-input-debug-visible])
        text-output-debug-visible (subscribe [:settings :text-output-debug-visible])
        selections-debug-visible (subscribe [:settings :selections-debug-visible])]
    (fn [editor-id]
      (log "R! editor-root" editor-id @state (= @state last-st) (identical? @state last-st))
      ;(if (= @state last-st) (js-debugger))
      (set! last-st @state)
      (let [{:keys [order]} @state
            {:keys [debug-parse-tree debug-text-input debug-text-output]} @state]
        [:div.plastic-editor                                ; .editor class is taken by Atom
         {:data-qeid editor-id
          :class     (classv (if @selections-debug-visible "debug-selections"))
          :on-click  (partial handle-editor-click editor-id)}
         (if @text-input-debug-visible [text-input-debug-component debug-text-input])
         [forms-component editor-id order]
         (if @parser-debug-visible [parser-debug-component debug-parse-tree])
         (if @text-output-debug-visible [text-output-debug-component debug-text-output])]))))

(defn mount-editor [element editor-id]
  (reagent/render [editor-root-component editor-id] element))

(defn unmount-editor [element]
  (reagent/unmount-component-at-node element))