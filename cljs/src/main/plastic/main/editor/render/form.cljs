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
  (let [root-id (id/make form-id :root)
        focused? (subscribe [:editor-focused-form-node editor-id form-id])
        layout (subscribe [:editor-layout-form-node editor-id form-id root-id])
        selection-subscription (subscribe [:editor-selection-node editor-id root-id])]
    (fn [editor-id form-id]
      (log-render "form" form-id
        (let [{:keys [tag selectable? sections form-kind]} @layout
              {:keys [headers docs code comments]} sections]
          [:div.form
           {:data-pnid form-id
            :class     (classv
                         (name tag)
                         (str "form-kind-" (name form-kind))
                         (if @focused? "focused")
                         (sections-to-class-names sections)
                         (if selectable? "selectable")
                         (if (and selectable? @selection-subscription) "selected"))
            :on-click  (partial handle-form-click form-id)}
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
