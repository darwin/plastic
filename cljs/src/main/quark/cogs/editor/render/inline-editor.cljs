(ns quark.cogs.editor.render.inline-editor
  (:require [quark.cogs.editor.render.utils :refer [raw-html wrap-specials classv]]
            [clojure.string :as string]
            [quark.onion.api :refer [$]]
            [quark.util.helpers :as helpers]
            [quark.cogs.editor.utils :as utils]
            [reagent.core :as reagent]
            [quark.onion.core :as onion])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn inline-editor-present? [$dom-node]
  (aget (.children $dom-node ".editor") 0))

(defn activate-inline-editor [$dom-node]
  (when-not (inline-editor-present? $dom-node)
    (let [editor-dom-node (aget (.closest $dom-node ".quark-editor") 0)
          _ (assert editor-dom-node)
          editor-id (.getAttribute editor-dom-node "data-qid")
          _ (assert editor-id)
          {:keys [editor view]} (onion/prepare-editor-instance (int editor-id))
          $view ($ view)]
      (log "activate inline editor")
      (.setUpdatedSynchronously view true)
      (.setText editor (.data $dom-node "text"))
      (.selectAll editor)
      (.setUpdatedSynchronously view false)
      (.append $dom-node view)
      ;(.show $view)
      (.focus view))))

(defn deactivate-inline-editor [$dom-node]
  (when-let [inline-editor-view (inline-editor-present? $dom-node)]
    (log "deactivate inline editor")
    (let [editor-dom-node (aget (.closest $dom-node ".quark-editor-view") 0)
          _ (assert editor-dom-node)]
      (log "focus" editor-dom-node)
      (.focus editor-dom-node))))

(defn inline-editor-action [action react-component]
  (let [dom-node (.getDOMNode react-component)              ; TODO: deprecated!
        _ (assert dom-node)
        $dom-node ($ dom-node)
        skelet-dom-node (aget (.closest $dom-node ".form-skelet") 0)]
    (if skelet-dom-node
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
    (fn [text]
      [:div.inline-editor
       {:data-text text}])))