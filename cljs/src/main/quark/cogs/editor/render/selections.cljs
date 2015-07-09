(ns quark.cogs.editor.render.selections
  (:require [quark.cogs.editor.render.code :refer [code-token-component]]
            [quark.cogs.editor.render.utils :refer [raw-html wrap-specials classv]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn selection-component [item]
  (let [{:keys [id geometry]} item
        {:keys [left top width height]} geometry]
    [:div.selection-placeholder
     {:style {:transform (str "translateY(" top "px) translateX(" left "px)")}}
     [:div.selection-ring
      [:div.selection-box
       {:data-id id
        :style   {:width  (str width "px")
                  :height (str height "px")}}]]]))

(defn form-selections-overlay-component []
  (fn [selections]
    [:div.form-overlay.form-selections-overlay
     (for [[id item] selections]
       ^{:key id}
       [selection-component item])]))