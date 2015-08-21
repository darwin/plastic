(ns plastic.main.editor.render.editor
  (:require-macros [plastic.logging :refer [log info warn error group group-end log-render]]
                   [plastic.main :refer [react! dispatch]])
  (:require [plastic.util.dom :as dom]
            [plastic.main.frame :refer [subscribe]]
            [plastic.main.editor.render.headers :refer [headers-group-component]]
            [plastic.main.editor.render.docs :refer [docs-group-component]]
            [plastic.main.editor.render.code :refer [code-box-component]]
            [plastic.main.editor.render.debug :refer [parser-debug-component text-input-debug-component text-output-debug-component render-tree-debug-component selections-debug-overlay-component]]
            [plastic.main.editor.render.utils :refer [dangerously-set-html classv]]
            [plastic.main.editor.toolkit.id :as id]))

(declare unified-rendering-component)

(defn render-tree-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-layout-form-node editor-id form-id node-id])
        selection-subscription (subscribe [:editor-selection-node editor-id node-id])]
    (fn [editor-id form-id node-id]
      (log-render "render-tree" node-id
        (let [{:keys [id tag children selectable?]} @layout]
          [:div {:data-qnid id
                 :class     (classv
                              (name tag)
                              (if selectable? "selectable")
                              (if (and selectable? @selection-subscription) "selected"))}
           (for [child-id children]
             ^{:key child-id} [unified-rendering-component editor-id form-id child-id])])))))

(defn unified-rendering-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-layout-form-node editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (let [{:keys [id tag]} @layout]
        (log-render "unified-rendering" node-id
          (if id
            [:div.unified {:data-qnid id}
             (condp = tag
               :tree [render-tree-component editor-id form-id id node-id]
               :code [code-box-component editor-id form-id node-id]
               :docs [docs-group-component editor-id form-id node-id]
               :headers [headers-group-component editor-id form-id node-id]
               (throw (str "don't know how to render tag " tag " (missing render component implementation)")))]
            [:div]))))))

(defn form-skelet-component [_editor-id _form-id]
  (fn [editor-id form-id]
    (log-render "form-skelet" form-id
      [:div.form-skelet
       [unified-rendering-component editor-id form-id (id/make form-id :root)]])))

(defn handle-form-click [_form-id event]
  (let [target-dom-node (.-target event)
        _ (assert target-dom-node)
        selectable-dom-node (dom/find-closest target-dom-node ".selectable")]
    (if selectable-dom-node
      (let [selected-node-id (dom/read-node-id selectable-dom-node)
            _ (assert selected-node-id)
            editor-id (dom/lookup-editor-id selectable-dom-node)]
        (.stopPropagation event)
        (if-not (dom/event-shift-key? event)
          (dispatch :editor-set-cursor editor-id selected-node-id true)
          (dispatch :editor-toggle-selection editor-id #{selected-node-id}))))))

(defn form-component [editor-id form-id]
  (let [focused-form? (subscribe [:editor-focused-form-node editor-id form-id])]
    (fn [editor-id form-id]
      (log-render "form" form-id
        [:tr
         [:td
          [:div.form
           {:data-qnid form-id
            :class     (if @focused-form? "focused")
            :on-click  (partial handle-form-click form-id)}
           [form-skelet-component editor-id form-id]]]]))))

(defn forms-component [_editor-id _order]
  (fn [editor-id order]
    (log-render "forms" editor-id
      [:table.form-table
       [:tbody
        (for [form-id order]
          ^{:key form-id} [form-component editor-id form-id])]])))

(defn handle-editor-click [editor-id event]
  (.stopPropagation event)
  (dispatch :editor-clear-selection editor-id)
  (dispatch :editor-clear-cursor editor-id))

(defn editor-root-component [editor-id]
  (let [state (subscribe [:editor-render-state editor-id])
        parser-debug-visible (subscribe [:settings :parser-debug-visible])
        text-input-debug-visible (subscribe [:settings :text-input-debug-visible])
        text-output-debug-visible (subscribe [:settings :text-output-debug-visible])
        selections-debug-visible (subscribe [:settings :selections-debug-visible])]
    (fn [editor-id]
      (log-render "editor-root" editor-id
        (let [{:keys [order]} @state
              {:keys [debug-parse-tree debug-text-input debug-text-output]} @state]
          [:div.plastic-editor                                                                                        ; .editor class is taken by Atom
           {:data-qeid editor-id
            :class     (classv (if @selections-debug-visible "debug-selections"))
            :on-click  (partial handle-editor-click editor-id)}
           (if @text-input-debug-visible [text-input-debug-component debug-text-input])
           [forms-component editor-id order]
           (if @parser-debug-visible [parser-debug-component debug-parse-tree])
           (if @text-output-debug-visible [text-output-debug-component debug-text-output])])))))
