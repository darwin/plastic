(ns plastic.main.editor.render.docs
  (:require [plastic.logging :refer-macros [log info warn error group group-end log-render]]
            [plastic.main.editor.render.utils :refer [dangerously-set-html wrap-specials fix-pre classv]]
            [plastic.main.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.main.editor.render.reusables :refer [raw-html-component]]
            [plastic.frame :refer [subscribe]]))

; -------------------------------------------------------------------------------------------------------------------

(defn doc-component [context editor-id unit-id node-id]
  (let [selected? (subscribe context [:editor-selection-node editor-id node-id])
        cursor? (subscribe context [:editor-cursor-node editor-id node-id])
        editing? (subscribe context [:editor-editing-node editor-id node-id])
        layout (subscribe context [:editor-layout-unit-node editor-id unit-id node-id])]
    (fn [_context _editor-id _unit-id _node-id]
      (let [layout @layout
            selected? @selected?
            editing? @editing?
            cursor? @cursor?]
        (log-render "doc" [node-id layout]
          (let [{:keys [text id selectable?]} layout]
            ^{:key id}
            [:div.doc
             [:div.docstring.token
              {:data-pnid id
               :class     (classv
                            (if (and (not editing?) selectable?) "selectable")
                            (if (and (not editing?) selectable? selected?) "selected")
                            (if cursor? "cursor")
                            (if editing? "editing"))}
              (if editing?
                [inline-editor-component context id]
                [raw-html-component context (fix-pre (wrap-specials text))])]]))))))

; -------------------------------------------------------------------------------------------------------------------

(defn docs-section-component [context editor-id unit-id node-id]
  (let [layout (subscribe context [:editor-layout-unit-node editor-id unit-id node-id])
        docs-visible (subscribe context [:settings :docs-visible])]
    (fn [context editor-id unit-id _node-id]
      (let [layout @layout]
        (log-render "docs-section" [node-id layout]
          [:div.docs-section
           (if @docs-visible
             (for [doc-id (:children layout)]
               ^{:key doc-id} [doc-component context editor-id unit-id doc-id]))])))))
