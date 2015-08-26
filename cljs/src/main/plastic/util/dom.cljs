(ns plastic.util.dom
  (:refer-clojure :exclude [find])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.dom.shim]
            [plastic.onion.api :refer [$]]
            [plastic.onion.atom :as atom]
            [plastic.main.editor.toolkit.id :as id]
            [clojure.string :as string]))

(defn node-from-react [react-component]
  (let [dom-node (.getDOMNode react-component)]                                                                       ; TODO: deprecated!
    (assert dom-node)
    dom-node))

(defn non-empty-result? [$node]
  (> (.-length $node) 0))

(defn single-result? [$node]
  (= (.-length $node) 1))

(defn has-class? [node class]
  {:pre [node]}
  (.hasClass ($ node) class))

(defn event-shift-key? [event]
  (.-shiftKey event))

(defn event-alt-key? [event]
  (.-altKey event))

(defn event-meta-key? [event]
  (.-metaKey event))

(defn event-ctrl-key? [event]
  (.-ctrlKey event))

(defn $->vec [$o]
  (let [len (alength $o)]
    (vec (map #(aget $o %) (range len)))))

(defn read-node-id [dom-node]
  (let [id (.data ($ dom-node) "qnid")]
    (assert (id/valid? id))
    id))

(defn read-editor-id [dom-node]
  (let [id (.data ($ dom-node) "qeid")]
    (assert (number? id))
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

(defn try-find
  ([$dom-node selector]
   (aget (.find $dom-node selector) 0))
  ([selector]
   (try-find $ selector)))

(defn find
  ([$dom-node selector]
   (let [result (try-find $dom-node selector)]
     (assert result)
     result))
  ([selector]
   (find $ selector)))

(defn find-all
  ([$dom-node selector]
   (.find $dom-node selector))
  ([selector]
   (find-all $ selector)))

(defn find-all-as-vec [& args]
  ($->vec (apply find-all args)))

(defn find-plastic-editor-view [editor-id]
  (atom/get-plastic-editor-view editor-id))

(defn find-plastic-editor [editor-id]
  (let [$editor-view ($ (find-plastic-editor-view editor-id))]
    (find $editor-view ".plastic-editor")))

(defn find-plastic-editor-form [editor-id form-id]
  (let [$editor ($ (find-plastic-editor editor-id))]
    (find $editor (str ".form[data-qnid=" form-id "]"))))

(defn skelet-node? [dom-node]
  (boolean (try-find-closest dom-node ".form-skelet")))

(defn inline-editor-present? [dom-node]
  (single-result? (.children ($ dom-node) "atom-text-editor")))

(defn build-nodes-selector [node-ids]
  (if-let [node-ids-seq (seq node-ids)]
    (string/join "," (map (fn [id] (str "[data-qnid=" id "]")) node-ids-seq))
    "[data-qnid=NONE]"))
