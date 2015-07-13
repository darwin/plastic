(ns plastic.cogs.editor.render.core
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]]
                   [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as reagent]
            [plastic.frame.core :refer [register-sub subscribe]]
            [plastic.util.dom-shim]
            [plastic.onion.api :refer [$]]
            [plastic.cogs.editor.render.headers :refer [headers-wrapper-component]]
            [plastic.cogs.editor.render.docs :refer [docs-component]]
            [plastic.cogs.editor.render.code :refer [code-box-component]]
            [plastic.cogs.editor.render.soup :refer [form-soup-overlay-component]]
            [plastic.cogs.editor.render.selections :refer [form-selections-overlay-component]]
            [plastic.cogs.editor.render.debug :refer [parser-debug-component text-input-debug-component text-output-debug-component render-tree-debug-component selections-debug-overlay-component]]
            [plastic.cogs.editor.render.utils :refer [dangerously-set-html classv]]
            [plastic.cogs.editor.render.dom :as dom]
            [plastic.util.helpers :as helpers]))

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

(defn render-tree-component []
  (fn [render-tree]
    (let [{:keys [id tag children selectable?]} render-tree]
      [:div {:data-qnid id
             :class     (classv
                          (name tag)
                          (if selectable? "selectable"))}
       (condp = tag
         :tree (for [child children]
                 ^{:key (or (:id child) (name (:tag child)))}
                 [render-tree-component child])
         :code [code-box-component render-tree]
         :docs [docs-component render-tree]
         :headers [headers-wrapper-component render-tree]
         (throw (str "don't know how to render tag " tag " (missing render component implementation)")))])))

(defn form-skelet-component []
  (form-scaffold
    (fn [render-tree]
      [:div.form-skelet
       [render-tree-component render-tree]])))

(defn handle-form-click [event]
  (let [target-dom-node (.-target event)
        _ (assert target-dom-node)
        selectable-dom-node (dom/find-closest target-dom-node ".selectable")]
    (if selectable-dom-node
      (let [selected-node-id (dom/read-node-id selectable-dom-node)
            _ (assert selected-node-id)
            editor-id (dom/lookup-editor-id selectable-dom-node)]
        (.stopPropagation event)
        (dispatch :editor-select (int editor-id) #{(int selected-node-id)})))))

(defn form-component []
  (let [selections-debug-visible (subscribe [:settings :selections-debug-visible])
        render-tree-debug-visible (subscribe [:settings :render-tree-debug-visible])]
    (fn [form]
      (log "R! form" (:id form))
      (let [{:keys [focused soup active-selections all-selections render-tree editing]} form]
        [:tr
         [:td
          [:div.form.noselect
           {:data-qnid (:id form)
            :class     (classv
                         (if focused "focused")
                         (if editing "editing"))
            :on-click  handle-form-click}
           [form-soup-overlay-component soup]
           [form-selections-overlay-component active-selections]
           (if @selections-debug-visible
             [selections-debug-overlay-component all-selections])
           [form-skelet-component render-tree]]
          (if @render-tree-debug-visible
            [render-tree-debug-component render-tree])]]))))

(defn forms-component []
  (fn [forms]
    [:table.form-table
     [:tbody
      (for [form forms]
        ^{:key (:id form)}
        [form-component form])]]))

(defn handle-editor-click [editor-id event]
  (.stopPropagation event)
  (dispatch :editor-clear-all-selections editor-id))

(defn editor-root-component [editor-id]
  (let [state (subscribe [:editor-render-state editor-id])
        parser-debug-visible (subscribe [:settings :parser-debug-visible])
        text-input-debug-visible (subscribe [:settings :text-input-debug-visible])
        text-output-debug-visible (subscribe [:settings :text-output-debug-visible])]
    (fn []
      (log "R! editor-root" editor-id)
      (let [forms (:forms @state)
            {:keys [debug-parse-tree debug-text-input debug-text-output]} @state]
        [:div.plastic-editor                                ; .editor class is taken by Atom
         {:data-qeid editor-id
          :on-click  (partial handle-editor-click editor-id)}
         (if @text-input-debug-visible [text-input-debug-component debug-text-input])
         [forms-component forms]
         (if @parser-debug-visible [parser-debug-component debug-parse-tree])
         (if @text-output-debug-visible [text-output-debug-component debug-text-output])]))))

(defn mount-editor [element editor-id]
  (let [editor (partial editor-root-component editor-id)]
    (reagent/render [editor] element)))