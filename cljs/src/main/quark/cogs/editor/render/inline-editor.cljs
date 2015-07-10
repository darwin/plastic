(ns quark.cogs.editor.render.inline-editor
  (:require [reagent.core :as reagent]
            [quark.onion.api :refer [$]]
            [quark.cogs.editor.render.utils :refer [dangerously-set-html wrap-specials classv]]
            [quark.cogs.editor.render.dom :as dom]
            [quark.onion.core :as onion])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]))

; inline-editor-component is only an empty shell for existing atom editor instance to be attached there
; shared atom editor instance is managed by onion (created outside clojurescript land, one instance per editor)
; hopefully transplanting one single atom editor instance around is faster than creating new ones

(defn activate-inline-editor [$dom-node]
  (when-not (dom/inline-editor-present? $dom-node)
    (let [editor-id (dom/lookup-editor-id $dom-node)
          inline-editor (onion/get-atom-inline-editor-instance editor-id)
          inline-editor-view (onion/get-atom-inline-editor-view-instance editor-id)
          editable-text (.data $dom-node "editable-text")]
      ; synchronous update prevent interminent jumps
      ; it has correct dimentsions before it gets appended in the DOM
      (.setUpdatedSynchronously inline-editor-view true)
      (.setText inline-editor editable-text)
      (.selectAll inline-editor)
      (.setUpdatedSynchronously inline-editor-view false)
      (.append $dom-node inline-editor-view)
      (.focus inline-editor-view))))

(defn deactivate-inline-editor [$dom-node]
  (if (dom/inline-editor-present? $dom-node)
    (let [editor-id (dom/lookup-editor-id $dom-node)
          inline-editor-view (onion/get-atom-inline-editor-view-instance editor-id)]
      (if (dom/has-class? inline-editor-view "is-focused")
        (let [root-view (dom/find-closest-quark-editor-view $dom-node)]
          (.focus root-view))))))

(defn inline-editor-transplantation [action react-component]
  (let [$dom-node ($ (dom/node-from-react react-component))]
    (if (dom/skelet-node? $dom-node)
      (condp = action
        :activate (activate-inline-editor $dom-node)
        :deactivate (deactivate-inline-editor $dom-node)))))

(defn inline-editor-scaffold [render-fn]
  (reagent/create-class
    {:component-did-mount    (partial inline-editor-transplantation :activate)
     :component-did-update   (partial inline-editor-transplantation :activate)
     :component-will-unmount (partial inline-editor-transplantation :deactivate)
     :reagent-render         render-fn}))

(defn inline-editor-component []
  (inline-editor-scaffold
    (fn [editable-text node-id]
      [:div.inline-editor
       {:data-editable-text editable-text
        :data-qnid          node-id}])))