(ns quark.cogs.editor.renderer
  (:require [reagent.core :as reagent]
            [quark.frame.core :refer [register-sub subscribe]]
            [quark.util.dom-shim]
            [quark.onion.api :refer [$]]
            [quark.cogs.editor.render.headers :refer [headers-wrapper-component]]
            [quark.cogs.editor.render.docs :refer [docs-wrapper-component]]
            [quark.cogs.editor.render.code :refer [code-wrapper-component]]
            [quark.cogs.editor.render.soup :refer [form-soup-overlay-component]]
            [quark.cogs.editor.render.selections :refer [form-selections-overlay-component]]
            [quark.cogs.editor.render.debug :refer [parser-debug-component plaintext-debug-component docs-debug-component code-debug-component headers-debug-component]]
            [quark.cogs.editor.render.utils :refer [raw-html classv]]
            [quark.util.helpers :as helpers])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [reagent.ratom :refer [reaction]]))

(defn retrieve-token-geometry [token-dom-node]
  (if-let [node-id (.getAttribute token-dom-node "data-qid")]
    [(int node-id) {:left (.-offsetLeft token-dom-node)
                    :top  (.-offsetTop token-dom-node)}]))

(defn retrieve-tokens-geometry [token-dom-nodes]
  (apply hash-map (mapcat retrieve-token-geometry token-dom-nodes)))

(defn retrieve-selectable-geometry [selectable-dom-node]
  (if-let [node-id (.getAttribute selectable-dom-node "data-qid")]
    [(int node-id) {:left   (.-offsetLeft selectable-dom-node)
                    :top    (.-offsetTop selectable-dom-node)
                    :width  (.-offsetWidth selectable-dom-node)
                    :height (.-offsetHeight selectable-dom-node)}]))

(defn retrieve-selectables-geometry [selectable-dom-nodes]
  (apply hash-map (mapcat retrieve-selectable-geometry selectable-dom-nodes)))

(defn capture-geometry [react-component]
  (let [dom-node (.getDOMNode react-component)              ; TODO: deprecated!
        _ (assert dom-node)
        form-node (aget (.closest ($ dom-node) ".form") 0)
        _ (assert form-node)
        form-id (.getAttribute form-node "data-qid")
        _ (assert form-id)
        editor-node (aget (.closest ($ form-node) ".quark-editor") 0)
        _ (assert editor-node)
        editor-id (.getAttribute editor-node "data-qid")
        _ (assert editor-id)
        token-dom-nodes (.getElementsByClassName dom-node "token")
        tokens-geometry (retrieve-tokens-geometry token-dom-nodes)
        selectable-dom-nodes (.getElementsByClassName dom-node "selectable")
        selectables-geometry (retrieve-selectables-geometry selectable-dom-nodes)]
    (dispatch :editor-update-soup-geometry (int editor-id) (int form-id) tokens-geometry)
    (dispatch :editor-update-selectables-geometry (int editor-id) (int form-id) selectables-geometry)))

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
        (log "R! form skelet" skelet)
        (let [{:keys [code-visible docs-visible
                      headers-debug-visible docs-debug-visible code-debug-visible plaintext-debug-visible]} @settings]
          [:div.form-skelet
           [headers-wrapper-component skelet]               ; headers are always visible
           (if docs-visible
             [docs-wrapper-component skelet])
           (if code-visible
             [code-wrapper-component skelet])
           (if plaintext-debug-visible
             [plaintext-debug-component skelet])
           (if headers-debug-visible
             [headers-debug-component skelet])
           (if docs-debug-visible
             [docs-debug-component skelet])
           (if code-debug-visible
             [code-debug-component skelet])])))))

(defn form-component []
  (fn [form]
    (log "R! form" (:id form) form)
    [:tr.form {:data-qid (:id form)}
     [:td.form-cell
      [:div.form-anchor
       [form-soup-overlay-component (:soup form)]
       [form-selections-overlay-component (:selections form)]
       [form-skelet-component (:skelet form)]]]]))

(defn forms-component []
  (fn [forms]
    (log "R! forms")
    [:table.form-group
     [:tbody
      (for [form forms]
        ^{:key (:id form)}
        [form-component form])]]))

(defn editor-root-component [editor-id]
  (let [state (subscribe [:editor-render-state editor-id])
        settings (subscribe [:settings])]
    (fn []
      (let [forms (:forms @state)
            {:keys [parser-debug-visible]} @settings
            parse-tree (if parser-debug-visible (:parse-tree @state) nil)]
        [:div.quark-editor {:data-qid editor-id}            ; editor class is taken by Atom
         [forms-component forms]
         (if parser-debug-visible
           [parser-debug-component parse-tree])]))))

(defn mount-editor [element editor-id]
  (let [editor (partial editor-root-component editor-id)]
    (reagent/render [editor] element)))