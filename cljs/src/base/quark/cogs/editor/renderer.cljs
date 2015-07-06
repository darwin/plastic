(ns quark.cogs.editor.renderer
  (:require [reagent.core :as reagent]
            [quark.frame.core :refer [register-sub subscribe]]
            [quark.cogs.editor.render.code :refer [code-component-wrapper]]
            [quark.cogs.editor.render.docs :refer [docs-component-wrapper]]
            [quark.cogs.editor.render.debug :refer [debug-component plain-text-component docs-debug-component code-debug-component]]
            [quark.cogs.editor.render.utils :refer [raw-html wrap-specials id! classv]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [reagent.ratom :refer [reaction]]))

(defn forms-component [forms]
  [:table.form-group
   (for [form forms]
     ^{:key (id!)} [:tr.form
                    ;[plain-text-component form]

                    [:td.docs-cell
                     [docs-component-wrapper form]]
                    [:td.code-cell
                     [code-component-wrapper form]]

                    ;[docs-debug-component form]
                    ;[code-debug-component form]
                    ;[debug-component form]
                    ;[soup-component form]
                    ])])

(defn editor-root-component [editor-id]
  (let [state (subscribe [:editor-render-state editor-id])]
    (fn []
      (let [forms (:forms @state)
            parse-tree (:parse-tree @state)]
        ^{:key (id!)} [:div.quark-editor-root
                       [forms-component forms]
                       ;[state-component parse-tree]
                       ]))))

(defn mount-editor [element editor-id]
  (let [editor (partial editor-root-component editor-id)]
    (reagent/render [editor] element)))