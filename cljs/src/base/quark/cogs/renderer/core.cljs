(ns quark.cogs.renderer.core
  (:require [reagent.core :as reagent]
            [quark.schema.paths :as paths]
            [quark.frame.core :refer [register-sub subscribe]]
            [quark.frame.middleware :refer [path]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [reagent.ratom :refer [reaction]]))

(register-sub :editor-render-state (fn [db [_query-id editor-id]]
                                     (reaction (get-in @db (concat paths/editors [editor-id :render-state])))))

(defn editor-root-component [editor-id]
  (let [state (subscribe [:editor-render-state editor-id])]
    (fn []
      [:div.fancy
       [:p "I am a component reading render-state!"]
       [:p (pr-str @state)]])))

(defn mount-editor [element editor-id]
  (let [editor (partial editor-root-component editor-id)]
    (reagent/render [editor] element)))