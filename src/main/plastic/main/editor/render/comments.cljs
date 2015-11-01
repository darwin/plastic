(ns plastic.main.editor.render.comments
  (:require [plastic.logging :refer-macros [log info warn error group group-end log-render]]
            [plastic.main.editor.render.utils :refer [dangerously-set-html wrap-specials fix-pre classv]]
            [plastic.main.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.main.editor.render.reusables :refer [raw-html-component]]
            [plastic.frame :refer [subscribe]]))

; -------------------------------------------------------------------------------------------------------------------

(defn comment-component [context editor-id unit-id node-id _spacing]
  (let [selected? (subscribe context [:editor-selection-node editor-id node-id])
        cursor? (subscribe context [:editor-cursor-node editor-id node-id])
        editing? (subscribe context [:editor-editing-node editor-id node-id])
        layout (subscribe context [:editor-layout-unit-node editor-id unit-id node-id])]
    (fn [context _editor-id _unit-id _node-id spacing]
      (let [layout @layout
            selected? @selected?
            editing? @editing?
            cursor? @cursor?]
        (log-render "comment" [node-id layout]
          (let [{:keys [text id selectable?]} layout]
            ^{:key id} [:div.comment-box {:style {:margin-top (str spacing "em")}}
                        [:div.comment.token
                         {:data-pnid id
                          :class     (classv
                                       (if (and (not editing?) selectable?) "selectable")
                                       (if (and (not editing?) selectable? selected?) "selected")
                                       (if cursor? "cursor")
                                       (if editing? "editing"))}
                         (if editing?
                           [inline-editor-component context id]
                           [raw-html-component context (fix-pre (wrap-specials text))])]]))))))

(defn comments-box-component [context editor-id unit-id node-id]
  (let [layout (subscribe context [:editor-layout-unit-node editor-id unit-id node-id])
        comments-visible (subscribe context [:settings :comments-visible])
        cursor? (subscribe context [:editor-cursor-node editor-id node-id])
        selected? (subscribe context [:editor-selection-node editor-id node-id])
        emitter (fn [comment-id spacing]
                  ^{:key comment-id} [comment-component context editor-id unit-id comment-id spacing])]
    (fn [_context _editor-id _unit-id _node-id]
      (let [layout @layout
            cursor? @cursor?
            selected? @selected?
            comments-visible @comments-visible]
        (log-render "comments-box" [node-id layout]
          (let [{:keys [id children metrics selectable?]} layout]
            [:div.comments-box {:data-pnid id
                                :class     (classv
                                             (if selectable? "selectable")
                                             (if (and selectable? selected?) "selected")
                                             (if cursor? "cursor"))}
             (if comments-visible
               (map emitter children metrics))]))))))
