(ns plastic.main.editor.render.form
  (:require-macros [plastic.logging :refer [log info warn error group group-end log-render]]
                   [plastic.main :refer [react! dispatch]])
  (:require [plastic.util.dom :as dom]
            [plastic.main.frame :refer [subscribe]]
            [plastic.main.editor.render.headers :refer [headers-section-component]]
            [plastic.main.editor.render.docs :refer [docs-section-component]]
            [plastic.main.editor.render.comments :refer [comments-box-component]]
            [plastic.main.editor.render.code :refer [code-box-component]]
            [plastic.main.editor.render.debug :refer [selections-debug-overlay-component]]
            [plastic.main.editor.render.utils :refer [dangerously-set-html classv sections-to-class-names]]
            [plastic.main.editor.toolkit.id :as id]))

(defn form-body-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-layout-form-node editor-id form-id node-id])
        selection-subscription (subscribe [:editor-selection-node editor-id node-id])]
    (fn [editor-id form-id node-id]
      (log-render "form-root" node-id
        (let [{:keys [id tag selectable? sections form-kind]} @layout
              {:keys [headers docs code comments]} sections]
          [:div.form-body {:data-pnid id
                           :class     (classv
                                        (name tag)
                                        (str "form-kind-" (name form-kind))
                                        (sections-to-class-names sections)
                                        (if selectable? "selectable")
                                        (if (and selectable? @selection-subscription) "selected"))}
           (if headers
             [headers-section-component editor-id form-id headers])
           (if docs
             [docs-section-component editor-id form-id docs])
           (if (or code comments)
             [:div.code-section
              (if code
                [code-box-component editor-id form-id code])
              (if comments
                [comments-box-component editor-id form-id comments])])])))))

(defn handle-form-click [_form-id event]
  (let [target-dom-node (.-target event)
        _ (assert target-dom-node)
        selectable-dom-node (dom/try-find-closest target-dom-node ".selectable")]
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
           {:data-pnid form-id
            :class     (classv
                         (if @focused-form? "focused"))
            :on-click  (partial handle-form-click form-id)}
           [form-body-component editor-id form-id (id/make form-id :root)]]]]))))
