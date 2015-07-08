(ns quark.cogs.editor.render.selections
  (:require [quark.cogs.editor.render.code :refer [code-token-component]]
            [quark.cogs.editor.render.utils :refer [raw-html wrap-specials classv]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn selection-component [item]
  (let [{:keys [id geometry]} item
        {:keys [left top width height]} geometry]
    [:div.selection {:data-id id
                     :style   {:transform (str "translateY(" top "px) translateX(" left "px)")
                               :width     (str width "px")
                               :height    (str height "px")}}]))

(defn form-selections-overlay-component []
  (fn [selections]
    (log "R! selections-overlay" selections)
    [:div.form-overlay.form-selections-overlay
     (for [item selections]
       ^{:key (:id item)}
       [selection-component item])]))