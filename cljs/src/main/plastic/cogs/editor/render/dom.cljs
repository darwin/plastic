(ns plastic.cogs.editor.render.dom
  (:refer-clojure :exclude [find])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.dom-shim]
            [plastic.onion.api :refer [$]]
            [plastic.cogs.editor.model :as editor]))

(defn node-from-react [react-component]
  (let [dom-node (.getDOMNode react-component)]             ; TODO: deprecated!
    (assert dom-node)
    dom-node))

(defn non-empty-result [$node]
  (> (.-length $node) 0))

(defn single-result? [$node]
  (= (.-length $node) 1))

(defn has-class? [node class]
  {:pre [node]}
  (.hasClass ($ node) class))

(defn valid-id? [id]
  (pos? id))

(defn read-node-id [dom-node]
  (let [id (int (.data ($ dom-node) "qnid"))]
    (assert (valid-id? id))
    id))

(defn read-editor-id [dom-node]
  (let [id (int (.data ($ dom-node) "qeid"))]
    (assert (valid-id? id))
    id))

(defn lookup-form-id [dom-node]
  (let [$form-dom-node (.closest ($ dom-node) ".form")
        _ (assert (single-result? $form-dom-node))]
    (read-node-id $form-dom-node)))

(defn lookup-editor-id [dom-node]
  (let [$editor-dom-node (.closest ($ dom-node) ".plastic-editor")
        _ (assert (single-result? $editor-dom-node))]
    (read-editor-id $editor-dom-node)))

(defn try-find-closest [dom-node selector]
  (aget (.closest ($ dom-node) selector) 0))

(defn find-closest [dom-node selector]
  (let [result (try-find-closest dom-node selector)]
    (assert result)
    result))

(defn find-closest-plastic-editor-view [dom-node]
  (find-closest dom-node ".plastic-editor-view"))

(defn try-find [selector]
  (aget (.find $ selector) 0))

(defn find [selector]
  (let [result (try-find selector)]
    (assert result)
    result))

(defn find-plastic-editor [editor-id]
  (find (str ".plastic-editor[data-qeid=" editor-id "]")))

(defn find-plastic-editor-view [editor-id]
  (find-closest-plastic-editor-view (find-plastic-editor editor-id)))

(defn skelet-node? [dom-node]
  (boolean (try-find-closest dom-node ".form-skelet")))

(defn postpone-selection-overlay-display-until-next-update [editor]
  (let [$root-view ($ (find-plastic-editor-view (editor/get-id editor)))]
    (.addClass $root-view "temporarily-hide-selection-overlay")))

(defn reenable-selection-overlay-display [dom-node]
  (let [$root-view ($ (find-closest-plastic-editor-view dom-node))]
    (.removeClass $root-view "temporarily-hide-selection-overlay")))

(defn inline-editor-present? [dom-node]
  (single-result? (.children ($ dom-node) "atom-text-editor")))
