(ns plastic.main.editor.render.unit
  (:require [plastic.logging :refer-macros [log info warn error group group-end log-render]]
            [plastic.util.dom :as dom]
            [plastic.frame :refer [subscribe] :refer-macros [dispatch]]
            [plastic.main.editor.render.headers :refer [headers-section-component]]
            [plastic.main.editor.render.docs :refer [docs-section-component]]
            [plastic.main.editor.render.comments :refer [comments-box-component]]
            [plastic.main.editor.render.code :refer [code-box-component]]
            [plastic.main.editor.render.debug :refer [selections-debug-overlay-component]]
            [plastic.main.editor.render.utils :refer [dangerously-set-html classv sections-to-class-names]]))

; -------------------------------------------------------------------------------------------------------------------

(defn handle-unit-click [context _unit-id event]
  (if-let [selectable-dom-node (dom/try-find-closest (.-target event) ".selectable")]
    (let [selected-node-id (dom/read-node-id selectable-dom-node)
          editor-id (dom/lookup-editor-id selectable-dom-node)]
      (assert selected-node-id)
      (.stopPropagation event)
      (if-not (dom/event-shift-key? event)
        (dispatch context [:editor-set-cursor editor-id selected-node-id true])
        (dispatch context [:editor-toggle-selection editor-id #{selected-node-id}])))))

(defn unit-component [context editor-id unit-id]
  (let [focused? (subscribe context [:editor-focused-unit-node editor-id unit-id])
        layout (subscribe context [:editor-layout-unit-node editor-id unit-id unit-id])
        selected? (subscribe context [:editor-selection-node editor-id unit-id])
        cursor? (subscribe context [:editor-cursor-node editor-id unit-id])]
    (fn [context editor-id unit-id]
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
              :on-click  (partial handle-unit-click context unit-id)}
             (if headers
               [headers-section-component context editor-id unit-id headers])
             (if docs
               [docs-section-component context editor-id unit-id docs])
             (if (or code comments)
               [:div.code-section
                (if code
                  [code-box-component context editor-id unit-id code])
                (if comments
                  [comments-box-component context editor-id unit-id comments])])]))))))
