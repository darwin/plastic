(ns plastic.cogs.editor.render.debug
  (:require [plastic.util.helpers :as helpers])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

(defn parser-debug-component [parse-tree]
  [:div.debug.parser-debug
   [:div (helpers/nice-print parse-tree)]])

(defn text-input-debug-component [plain-text]
  [:div.debug.text-input-debug
   [:div plain-text]])

(defn text-output-debug-component [plain-text]
  [:div.debug.text-output-debug
   [:div plain-text]])

(defn render-tree-debug-component [render-tree]
  [:div.debug.render-tree-debug
   [:div (helpers/nice-print render-tree)]])

(defn selection-component [item]
  (let [{:keys [id geometry]} item
        {:keys [left top width height]} geometry]
    [:div.debug-selection {:data-id id
                              :style   {:transform (str "translateY(" top "px) translateX(" left "px)")
                                        :width     (str width "px")
                                        :height    (str height "px")}}]))

(defn selections-debug-overlay-component []
  (fn [selections]
    [:div.form-selections-debug-overlay
     (for [item (vals selections)]
       ^{:key (:id item)}
       [selection-component item])]))