(ns quark.cogs.editor.render.selections
  (:require [quark.cogs.editor.render.code :refer [code-token-component]]
            [quark.cogs.editor.render.utils :refer [classv]]
            [quark.cogs.editor.dom :refer [dom-node-from-react]]
            [reagent.core :as reagent])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]))

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

(defn announce-selections-ready [react-component]
  (let [dom-node (dom-node-from-react react-component)]
    ))

(defn selections-overlay-scaffold [render-fn]
  (reagent/create-class
    {:component-did-mount  (fn [& args] (apply announce-selections-ready args))
     :component-did-update (fn [& args] (apply announce-selections-ready args))
     :reagent-render       render-fn}))

(defn form-selections-overlay-component []
  (selections-overlay-scaffold
    (fn [selections]
      [:div.form-overlay.form-selections-overlay
       (for [[id item] selections]
         ^{:key id}
         [selection-component item])])))