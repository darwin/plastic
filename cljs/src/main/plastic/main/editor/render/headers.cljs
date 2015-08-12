(ns plastic.main.editor.render.headers
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main.render :refer [log-render]])
  (:require [plastic.main.editor.render.utils :refer [wrap-specials classv]]
            [plastic.main.frame.core :refer [subscribe]]))

(defn header-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-form-node-layout editor-id form-id node-id])]
    (fn [_editor-id _form-id node-id]
      (log-render "header" node-id
        (let [{:keys [text id]} @layout]
          ^{:key id}
          [:div.header
           [:div.name [:div text]]])))))

(defn headers-group-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-form-node-layout editor-id form-id node-id])]
    (fn [editor-id form-id node-id]
      (log-render "headers-group" node-id
        [:div.headers-group
         (for [header-id (:children @layout)]
           ^{:key header-id} [header-component editor-id form-id header-id])]))))