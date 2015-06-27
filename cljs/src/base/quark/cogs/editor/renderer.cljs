(ns quark.cogs.editor.renderer
  (:require [reagent.core :as reagent]
            [quark.frame.core :refer [register-sub subscribe]]
            [quark.frame.middleware :refer [path]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [reagent.ratom :refer [reaction]]))

(defn editor-root-component [editor-id]
  (let [state (subscribe [:editor-render-state editor-id])]
    (fn []
      (let [layout (:layout @state)]
        [:div
         (for [item layout]
           ^{:key item} [:div.fancy
            [:div item]])]))))

(defn mount-editor [element editor-id]
  (let [editor (partial editor-root-component editor-id)]
    (reagent/render [editor] element)))