(ns quark.cogs.editor.render.inline-editor
  (:require [reagent.core :as reagent]
            [quark.onion.api :refer [$]]
            [quark.cogs.editor.render.utils :refer [dangerously-set-html wrap-specials classv]]
            [quark.cogs.editor.render.dom :refer [skelet-node? find-closest-quark-editor-view single-result? lookup-form-id lookup-editor-id dom-node-from-react read-node-id find-closest]]
            [quark.onion.core :as onion])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]))

(defn inline-editor-present? [$dom-node]
  (single-result? (.children $dom-node ".editor")))

(defn activate-inline-editor [$dom-node]
  (when-not (inline-editor-present? $dom-node)
    (let [editor-id (lookup-editor-id $dom-node)
          {:keys [editor view]} (onion/prepare-editor-instance editor-id)]
      (log "activate inline editor")
      (.setUpdatedSynchronously view true)
      (.setText editor (.data $dom-node "text"))
      (.selectAll editor)
      (.setUpdatedSynchronously view false)
      (.append $dom-node view)
      (.focus view))))

(defn deactivate-inline-editor [$dom-node]
  (if (inline-editor-present? $dom-node)
    (let [root-view (find-closest-quark-editor-view $dom-node)
          editor-id (lookup-editor-id $dom-node)
          form-id (lookup-form-id $dom-node)
          node-id (read-node-id $dom-node)
          {:keys [editor]} (onion/prepare-editor-instance editor-id)
          value (.getText editor)]
      (log "deactivate inline editor")
      (.focus root-view))))

(defn inline-editor-action [action react-component]
  (let [$dom-node ($ (dom-node-from-react react-component))]
    (if (skelet-node? $dom-node)
      (condp = action
        :activate (activate-inline-editor $dom-node)
        :deactivate (deactivate-inline-editor $dom-node)))
    nil))

(defn inline-editor-scaffold [render-fn]
  (reagent/create-class
    {:component-did-mount    (partial inline-editor-action :activate)
     :component-did-update   (partial inline-editor-action :activate)
     :component-will-unmount (partial inline-editor-action :deactivate)
     :reagent-render         render-fn}))

(defn inline-editor-component []
  (inline-editor-scaffold
    (fn [text node-id]
      [:div.inline-editor
       {:data-text text
        :data-qnid node-id}])))
