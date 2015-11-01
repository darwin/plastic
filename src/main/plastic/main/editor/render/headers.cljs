(ns plastic.main.editor.render.headers
  (:require [plastic.logging :refer-macros [log info warn error group group-end log-render]]
            [plastic.main.editor.render.utils :refer [wrap-specials classv]]
            [plastic.frame :refer [subscribe]]))

; -------------------------------------------------------------------------------------------------------------------

(defn header-component [context editor-id unit-id node-id]
  (let [layout (subscribe context [:editor-layout-unit-node editor-id unit-id node-id])]
    (fn [_context _editor-id _unit-id _node-id]
      (let [layout @layout]
        (log-render "header" [node-id layout]
          (let [{:keys [text arities id]} layout]
            ^{:key id}
            [:div.header
             [:div.name [:div text]]
             [:div.arities {:class (str "arity-" (count arities))}
              (for [args arities]
                ^{:key (goog/getUid args)} [:div.args args])]]))))))

(defn headers-section-component [context editor-id unit-id node-id]
  (let [layout (subscribe context [:editor-layout-unit-node editor-id unit-id node-id])
        headers-visible (subscribe context [:settings :headers-visible])
        emitter (fn [header-id]
                  ^{:key header-id} [header-component context editor-id unit-id header-id])]
    (fn [_context _editor-id _unit-id _node-id]
      (let [layout @layout]
        (log-render "headers-section" [node-id layout]
          (let [{:keys [children]} layout]
            [:div.headers-section
             (if @headers-visible
               (map emitter children))]))))))
