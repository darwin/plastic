(ns plastic.main.editor.render.unit
  (:require-macros [plastic.logging :refer [log info warn error group group-end log-render]]
                   [plastic.main :refer [dispatch]])
  (:require [plastic.util.dom :as dom]
            [plastic.main.frame :refer [subscribe]]
            [plastic.main.editor.render.headers :refer [headers-section-component]]
            [plastic.main.editor.render.docs :refer [docs-section-component]]
            [plastic.main.editor.render.comments :refer [comments-box-component]]
            [plastic.main.editor.render.code :refer [code-box-component]]
            [plastic.main.editor.render.debug :refer [selections-debug-overlay-component]]
            [plastic.main.editor.render.utils :refer [dangerously-set-html classv sections-to-class-names]]))

(defn handle-unit-click [_unit-id event]
  (if-let [selectable-dom-node (dom/try-find-closest (.-target event) ".selectable")]
    (let [selected-node-id (dom/read-node-id selectable-dom-node)
          editor-id (dom/lookup-editor-id selectable-dom-node)]
      (assert selected-node-id)
      (.stopPropagation event)
      (if-not (dom/event-shift-key? event)
        (dispatch :editor-set-cursor editor-id selected-node-id true)
        (dispatch :editor-toggle-selection editor-id #{selected-node-id})))))

(defn unit-component [editor-id unit-id]
  (let [focused? (subscribe [:editor-focused-unit-node editor-id unit-id])
        layout (subscribe [:editor-layout-unit-node editor-id unit-id unit-id])
        selected? (subscribe [:editor-selection-node editor-id unit-id])
        cursor? (subscribe [:editor-cursor-node editor-id unit-id])]
    (fn [editor-id unit-id]
      (let [layout @layout
            selected? @selected?
            focused? @focused?
            cursor? @cursor?]
        (log-render "unit" [unit-id layout]
          (let [{:keys [selectable? sections unit-type unit-kind]} layout
                {:keys [headers docs code comments]} sections]
            [:div.unit
             {:data-pnid unit-id
              :class     (classv
                           (str "unit-type-" unit-type)
                           (if unit-kind (str "unit-kind-" unit-kind))
                           (sections-to-class-names sections)
                           (if focused? "focused")
                           (if cursor? "cursor")
                           (if selectable? "selectable")
                           (if (and selectable? selected?) "selected"))
              :on-click  (partial handle-unit-click unit-id)}
             (if headers
               [headers-section-component editor-id unit-id headers])
             (if docs
               [docs-section-component editor-id unit-id docs])
             (if (or code comments)
               [:div.code-section
                (if code
                  [code-box-component editor-id unit-id code])
                (if comments
                  [comments-box-component editor-id unit-id comments])])]))))))
