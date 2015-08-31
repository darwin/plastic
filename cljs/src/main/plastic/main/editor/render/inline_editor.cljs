(ns plastic.main.editor.render.inline-editor
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.main :refer [react! dispatch]])
  (:require [reagent.core :as reagent]
            [plastic.onion.api :refer [$]]
            [plastic.util.dom :as dom]
            [plastic.onion.atom.inline-editor :as inline-editor]))

; inline-editor-component is only an empty shell for existing atom editor instance to be attached there.
; Shared atom editor instance is managed by onion (created outside clojurescript land, one instance per editor).
; Hopefully transplanting one single atom editor element around is faster than always creating a new one.

(def log-label "INLINE EDITOR")

(defn activate-transplantation [$dom-node]
  (if plastic.env.log-inline-editor
    (fancy-log log-label "activate transplantation to" $dom-node "activeElement:" (.-activeElement js/document)))
  (let [editor-id (dom/lookup-editor-id $dom-node)]
    (inline-editor/append-inline-editor editor-id $dom-node)
    (inline-editor/focus-inline-editor editor-id)
    (if plastic.env.log-inline-editor
      (fancy-log log-label "activeElement after transplantation:" (.-activeElement js/document)))))

; inline-editor element will be eventually removed from the dom by react
; plastic.onion.atom.inline-editor still holds reference to that element so it can re-append it somewhere else
;
; note: I know what you are probably asking:
;       "wouldn't it be symmetric to detach inline-editor-element and return focus to roo-view here?"
;
;       Trying to remove element and append it to parking position proved to be tricky here.
;       In one rendering update, one node can lose editing status and another can gain it,
;       so we receive multiple activate/deactivate calls during this one render update.
;       But react/reagent does not guarantee ordering of those calls - it depends on position of affected
;       nodes in the dom tree. We would have to queue them and do deactivation first and activation later.
;       And by the time we get to do queued deactivation, the node would be probably detached anyways.
;
(defn deactivate-transplantation [$dom-node]
  (let [editor-id (dom/lookup-editor-id $dom-node)
        $root-view ($ (dom/find-closest-plastic-editor-view $dom-node))]
    (when (inline-editor/is-inline-editor-focused? editor-id)
      (if plastic.env.log-inline-editor
        (fancy-log log-label "returning focus back to root-view"))
      (.focus $root-view))))

(defn transplant-inline-editor [action react-component]
  (let [$dom-node ($ (dom/node-from-react react-component))]
    (condp = action
      :activate (activate-transplantation $dom-node)
      :deactivate (deactivate-transplantation $dom-node))))

(defn inline-editor-class [render-fn]
  (reagent/create-class
    {:component-did-mount    (partial transplant-inline-editor :activate)
     :component-did-update   (partial transplant-inline-editor :activate)
     :component-will-unmount (partial transplant-inline-editor :deactivate)
     :reagent-render         render-fn}))

(defn inline-editor-component []
  (inline-editor-class
    (fn [node-id]
      [:div.inline-editor
       {:data-pnid node-id}])))
