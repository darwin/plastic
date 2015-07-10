(ns quark.cogs.editor.dom
  (:require [quark.util.dom-shim]
            [quark.onion.api :refer [$]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn dom-node-from-react [react-component]
  (let [dom-node (.getDOMNode react-component)]             ; TODO: deprecated!
    (assert dom-node)
    dom-node))

(defn non-empty-result [$node]
  (> (.-length $node) 0))

(defn single-result? [$node]
  (= (.-length $node) 1))

(defn read-node-id [dom-node]
  (int (.data ($ dom-node) "qnid")))

(defn read-editor-id [dom-node]
  (int (.data ($ dom-node) "qeid")))

(defn lookup-form-id [dom-node]
  (let [$form-dom-node (.closest ($ dom-node) ".form")
        _ (assert (single-result? $form-dom-node))]
    (read-node-id $form-dom-node)))

(defn lookup-editor-id [dom-node]
  (let [$editor-dom-node (.closest ($ dom-node) ".quark-editor")
        _ (assert (single-result? $editor-dom-node))]
    (read-editor-id $editor-dom-node)))

(defn try-find-closest [dom-node selector]
  (let [$match (.closest ($ dom-node) selector)]
    (aget $match 0)))

(defn find-closest [dom-node selector]
  (let [result (try-find-closest dom-node selector)]
    (assert result)
    result))

(defn find-closest-quark-editor-view [dom-node]
  (find-closest dom-node ".quark-editor-view"))

(defn skelet-node? [dom-node]
  (boolean (try-find-closest dom-node ".form-skelet")))