(ns plastic.main.editor.render.inline-editor
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch]])
  (:require [reagent.core :as reagent]
            [plastic.onion.api :refer [$]]
            [plastic.main.editor.render.dom :as dom]
            [plastic.onion.atom :as onion]))

; inline-editor-component is only an empty shell for existing atom editor instance to be attached there
; shared atom editor instance is managed by onion (created outside clojurescript land, one instance per editor)
; hopefully transplanting one single atom editor instance around is faster than always creating new ones

(defn activate-inline-editor [$dom-node]
  (when-not (dom/inline-editor-present? $dom-node)
    (let [editor-id (dom/lookup-editor-id $dom-node)
          inline-editor-mode (keyword (.data $dom-node "qmode"))
          inline-editor-text (str (.data $dom-node "qtext"))
          root-view (dom/find-closest-plastic-editor-view $dom-node)]
      (onion/setup-inline-editor-for-editing editor-id inline-editor-mode inline-editor-text)
      (onion/append-inline-editor editor-id $dom-node)
      (onion/focus-inline-editor editor-id)
      (.addClass ($ root-view) "inline-editor-active"))))

(defn deactivate-inline-editor [$dom-node]
  (if (dom/inline-editor-present? $dom-node)
    (let [editor-id (dom/lookup-editor-id $dom-node)]
      (if (onion/is-inline-editor-focused? editor-id)
        (let [root-view (dom/find-closest-plastic-editor-view $dom-node)]
          (.focus root-view)
          (.removeClass ($ root-view) "inline-editor-active"))))))

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
    (fn [node-id text mode]
      [:div.inline-editor
       {:data-qnid  node-id
        :data-qtext text
        :data-qmode mode}])))