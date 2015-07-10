(ns plastic.cogs.editor.renderer
  (:require [reagent.core :as reagent]
            [plastic.frame.core :refer [register-sub subscribe]]
            [plastic.util.dom-shim]
            [plastic.onion.api :refer [$]]
            [plastic.cogs.editor.render.headers :refer [headers-wrapper-component]]
            [plastic.cogs.editor.render.docs :refer [docs-wrapper-component]]
            [plastic.cogs.editor.render.code :refer [code-wrapper-component]]
            [plastic.cogs.editor.render.soup :refer [form-soup-overlay-component]]
            [plastic.cogs.editor.render.selections :refer [form-selections-overlay-component]]
            [plastic.cogs.editor.render.debug :refer [parser-debug-component plaintext-debug-component docs-debug-component code-debug-component headers-debug-component selections-debug-overlay-component]]
            [plastic.cogs.editor.render.utils :refer [dangerously-set-html classv]]
            [plastic.cogs.editor.render.dom :as dom]
            [plastic.util.helpers :as helpers])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]]
                   [reagent.ratom :refer [reaction]]))

(defn retrieve-token-geometry [token-dom-node]
  (if-let [node-id (dom/read-node-id token-dom-node)]
    [node-id {:left (.-offsetLeft token-dom-node)
              :top  (.-offsetTop token-dom-node)}]))

(defn retrieve-tokens-geometry [token-dom-nodes]
  (apply hash-map (mapcat retrieve-token-geometry token-dom-nodes)))

(defn retrieve-selectable-geometry [selectable-dom-node]
  (if-let [node-id (dom/read-node-id selectable-dom-node)]
    [node-id {:left   (.-offsetLeft selectable-dom-node)
              :top    (.-offsetTop selectable-dom-node)
              :width  (.-offsetWidth selectable-dom-node)
              :height (.-offsetHeight selectable-dom-node)}]))

(defn retrieve-selectables-geometry [selectable-dom-nodes]
  (apply hash-map (mapcat retrieve-selectable-geometry selectable-dom-nodes)))

(defn capture-geometry [react-component]
  (let [dom-node (dom/node-from-react react-component)
        form-id (dom/lookup-form-id dom-node)
        editor-id (dom/lookup-editor-id dom-node)]
    (let [selectable-dom-nodes (.getElementsByClassName dom-node "selectable")
          selectables-geometry (retrieve-selectables-geometry selectable-dom-nodes)]
      (dispatch :editor-update-selectables-geometry (int editor-id) (int form-id) selectables-geometry))
    (let [token-dom-nodes (.getElementsByClassName dom-node "token")
          tokens-geometry (retrieve-tokens-geometry token-dom-nodes)]
      (dispatch :editor-update-soup-geometry (int editor-id) (int form-id) tokens-geometry))))

(defn form-scaffold [render-fn]
  (let [debounced-capture-geometry (helpers/debounce capture-geometry 30)]
    (reagent/create-class
      {:component-did-mount  (fn [& args] (apply debounced-capture-geometry args))
       :component-did-update (fn [& args] (apply debounced-capture-geometry args))
       :reagent-render       render-fn})))

(defn form-skelet-component []
  (let [settings (subscribe [:settings])]
    (form-scaffold
      (fn [skelet]
        (let [{:keys [code-visible docs-visible
                      headers-debug-visible docs-debug-visible code-debug-visible plaintext-debug-visible]} @settings]
          [:div.form-skelet
           [headers-wrapper-component skelet]               ; headers are always visible
           (if docs-visible [docs-wrapper-component skelet])
           (if code-visible [code-wrapper-component skelet])
           (if plaintext-debug-visible [plaintext-debug-component skelet])
           (if headers-debug-visible [headers-debug-component skelet])
           (if docs-debug-visible [docs-debug-component skelet])
           (if code-debug-visible [code-debug-component skelet])])))))

(defn handle-form-click [form event]
  (let [target-dom-node (.-target event)
        _ (assert target-dom-node)
        selectable-dom-node (dom/find-closest target-dom-node ".selectable")]
    (if selectable-dom-node
      (let [selected-node-id (dom/read-node-id selectable-dom-node)
            _ (assert selected-node-id)
            editor-id (dom/lookup-editor-id selectable-dom-node)]
        (.stopPropagation event)
        (dispatch :editor-select (int editor-id) (:id form) #{(int selected-node-id)})))))

(defn form-component []
  (let [settings (subscribe [:settings])]
    (fn [form]
      (log "R! form" (:id form))
      (let [{:keys [selections-debug-visible]} @settings
            {:keys [focused soup active-selections all-selections skelet editing]} form]
        [:tr.form-row
         [:td.form-cell
          [:div.form.noselect
           {:data-qnid (:id form)
            :class    (classv
                        (if focused "focused")
                        (if editing "editing"))
            :on-click (partial handle-form-click form)}
           [form-soup-overlay-component soup]
           [form-selections-overlay-component active-selections]
           (if selections-debug-visible
             [selections-debug-overlay-component all-selections])
           [form-skelet-component skelet]]]]))))

(defn forms-component []
  (fn [forms]
    [:table.form-group
     [:tbody
      (for [form forms]
        ^{:key (:id form)}
        [form-component form])]]))

(defn handle-editor-click [editor-id event]
  (.stopPropagation event)
  (dispatch :editor-clear-all-selections editor-id))

(defn editor-root-component [editor-id]
  (let [state (subscribe [:editor-render-state editor-id])
        settings (subscribe [:settings])]
    (fn []
      (log "R! editor-root" editor-id)
      (let [forms (:forms @state)
            {:keys [parser-debug-visible]} @settings
            parse-tree (if parser-debug-visible (:debug-parse-tree @state) nil)]
        [:div.plastic-editor                                  ; .editor class is taken by Atom
         {:data-qeid editor-id
          :on-click (partial handle-editor-click editor-id)}
         [forms-component forms]
         (if parser-debug-visible [parser-debug-component parse-tree])]))))

(defn mount-editor [element editor-id]
  (let [editor (partial editor-root-component editor-id)]
    (reagent/render [editor] element)))