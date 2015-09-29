(ns plastic.main.editor.render.debug
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]))

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
