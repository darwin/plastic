(ns quark.cogs.editor.renderer
  (:require [reagent.core :as reagent]
            [quark.frame.core :refer [register-sub subscribe]]
            [quark.frame.middleware :refer [path]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [reagent.ratom :refer [reaction]]))

(defn plain-text-compoent [form]
  [:div.fancy
   [:div (:text form)]])

(defn state-component [form]
  [:div.state
   [:div (pr-str (:soup form))]])

(defn editor-component [form]
  [:div.soup
   (for [item (:soup form)]
     (if (= (:tag item) :newline)
       [:br]
       ^{:key (:path item)}
         [:div.soup-item
          (:string item)]))])

(defn editor-root-component [editor-id]
  (let [state (subscribe [:editor-render-state editor-id])]
    (fn []
      (let [forms (:forms @state)]
        [:div
         (for [form forms]
           ^{:key form}
           [:div
            [plain-text-compoent form]
            [editor-component form]
            [state-component form]])]))))

(defn mount-editor [element editor-id]
  (let [editor (partial editor-root-component editor-id)]
    (reagent/render [editor] element)))