(ns plastic.main.editor.render.headers
  (:require-macros [plastic.logging :refer [log info warn error group group-end log-render]])
  (:require [plastic.main.editor.render.utils :refer [wrap-specials classv]]
            [plastic.main.frame :refer [subscribe]]))

(defn header-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-layout-form-node editor-id form-id node-id])]
    (fn [_editor-id _form-id node-id]
      (log-render "header" node-id
        (let [{:keys [text arities id]} @layout]
          ^{:key id}
          [:div.header
           [:div.name [:div text]]
           [:div.arities {:class (str "arity-" (count arities))}
            (for [args arities]
              ^{:key (goog/getUid args)} [:div.args args])]])))))

(defn headers-section-component [editor-id form-id node-id]
  (let [layout (subscribe [:editor-layout-form-node editor-id form-id node-id])
        headers-visible (subscribe [:settings :headers-visible])
        emitter (fn [header-id]
                  ^{:key header-id} [header-component editor-id form-id header-id])]
    (fn [_editor-id _form-id node-id]
      (log-render "headers-section" node-id
        (let [{:keys [children]} @layout]
          [:div.headers-section
           (if @headers-visible
             (map emitter children))])))))
