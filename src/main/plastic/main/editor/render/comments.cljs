(ns plastic.main.editor.render.comments
  (:require-macros [plastic.logging :refer [log info warn error group group-end log-render]])
  (:require [plastic.main.editor.render.utils :refer [dangerously-set-html wrap-specials fix-pre classv]]
            [plastic.main.editor.render.inline-editor :refer [inline-editor-component]]
            [plastic.main.editor.render.reusables :refer [raw-html-component]]
            [plastic.main.frame :refer [subscribe]]))

(defn comment-component [editor-id unit-id node-id _spacing]
  (let [selected? (subscribe [:editor-selection-node editor-id node-id])
        cursor? (subscribe [:editor-cursor-node editor-id node-id])
        editing? (subscribe [:editor-editing-node editor-id node-id])
        layout (subscribe [:editor-layout-unit-node editor-id unit-id node-id])]
    (fn [_editor-id _unit-id _node-id spacing]
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
                           [inline-editor-component id]
                           [raw-html-component (fix-pre (wrap-specials text))])]]))))))

(defn comments-box-component [editor-id unit-id node-id]
  (let [layout (subscribe [:editor-layout-unit-node editor-id unit-id node-id])
        comments-visible (subscribe [:settings :comments-visible])
        cursor? (subscribe [:editor-cursor-node editor-id node-id])
        selected? (subscribe [:editor-selection-node editor-id node-id])
        emitter (fn [comment-id spacing]
                  ^{:key comment-id} [comment-component editor-id unit-id comment-id spacing])]
    (fn [_editor-id _unit-id _node-id]
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