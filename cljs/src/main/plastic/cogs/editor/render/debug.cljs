(ns plastic.cogs.editor.render.debug
  (:require [plastic.util.helpers :as helpers])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

(defn plaintext-debug-component [form-render-info]
  [:div.plain-text
   [:div (:text form-render-info)]])

(defn parser-debug-component [parse-tree]
  [:div.state
   [:div (helpers/nice-print parse-tree)]])

(defn code-debug-component [form]
  [:div.code-debug
   [:div (helpers/nice-print (:code form))]])

(defn docs-debug-component [form]
  [:div.docs-debug
   [:div (helpers/nice-print (:docs form))]])

(defn headers-debug-component [form]
  [:div.headers-debug
   [:div (helpers/nice-print (:headers form))]])

(defn debug-component [form]
  [:div.debug
   [:div (helpers/nice-print (:debug form))]])

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