(ns quark.cogs.editor.renderer
  (:require [reagent.core :as reagent]
            [quark.frame.core :refer [register-sub subscribe]]
            [quark.cogs.editor.render.code :refer [code-wrapper-component]]
            [quark.cogs.editor.render.docs :refer [docs-wrapper-component]]
            [quark.cogs.editor.render.debug :refer [parser-debug-component plaintext-debug-component docs-debug-component code-debug-component]]
            [quark.cogs.editor.render.utils :refer [raw-html wrap-specials id! classv]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [reagent.ratom :refer [reaction]]))

(defn forms-component []
  (let [settings (subscribe [:settings])]
    (fn [forms]
      (let [{:keys [code-visible docs-visible
                    docs-debug-visible code-debug-visible plaintext-debug-visible]} @settings]
        [:table.form-group
         [:tbody
          (for [form forms]
            ^{:key (id!)} [:tr.form
                           [:td.form-cell
                            [:div.form-box
                             (if plaintext-debug-visible
                               [plaintext-debug-component form])
                             (if docs-visible
                               [docs-wrapper-component form])
                             (if code-visible
                               [code-wrapper-component form])
                             (if docs-debug-visible
                               [docs-debug-component form])
                             (if code-debug-visible
                               [code-debug-component form])]]])]]))))

(defn editor-root-component [editor-id]
  (let [state (subscribe [:editor-render-state editor-id])
        settings (subscribe [:settings])]
    (fn []
      (let [forms (:forms @state)
            {:keys [parser-debug-visible]} @settings
            parse-tree (if parser-debug-visible (:parse-tree @state) nil)]
        ^{:key (id!)} [:div.quark-editor-root
                       [forms-component forms]
                       (if parser-debug-visible
                         [parser-debug-component parse-tree])]))))

(defn mount-editor [element editor-id]
  (let [editor (partial editor-root-component editor-id)]
    (reagent/render [editor] element)))