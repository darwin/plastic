(ns plastic.util.dom
  (:refer-clojure :exclude [find])
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.dom.shim]
            [plastic.onion.api :refer [$]]
            [plastic.onion.host :as atom]
            [plastic.main.editor.toolkit.id :as id]
            [clojure.string :as string]
            [clojure.string :as str]))

; -------------------------------------------------------------------------------------------------------------------

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
  (let [id (.data ($ dom-node) "pnid")]
    (assert (id/valid? id))
    id))

(defn read-editor-id [dom-node]
  (let [id (.data ($ dom-node) "peid")]
    (assert (number? id))
    id))

(defn lookup-unit-id [dom-node]
  (let [$unit-dom-node (.closest ($ dom-node) ".unit")
        _ (assert (single-result? $unit-dom-node))]
    (read-node-id $unit-dom-node)))

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

(defn inline-editor-present? [dom-node]
  (single-result? (.children ($ dom-node) "atom-text-editor")))

(defn build-nodes-selector [node-ids]
  (if-let [node-ids-seq (seq node-ids)]
    (string/join "," (map (fn [id] (str "[data-pnid=" id "]")) node-ids-seq))
    "[data-pnid=NONE]"))

(defn escape-html [text]
  (-> text
    (str/replace "&" "&amp;")
    (str/replace "<" "&lt;")
    (str/replace ">" "&gt;")
    (str/replace "\"" "&quot;")))

(defn build-plastic-editor-view-selector [editor-id]
  (str ".plastic-editor-view[data-pevid=" editor-id "]"))

(defn find-plastic-editor-view [editor-id]
  (let [plastic-editor-view (find (build-plastic-editor-view-selector editor-id))]
    (assert plastic-editor-view)
    plastic-editor-view))

(defn find-plastic-editor [editor-id]
  (let [$editor-view ($ (find-plastic-editor-view editor-id))]
    (find $editor-view ".plastic-editor")
    $editor-view))

(defn find-plastic-editor-unit [editor-id unit-id]
  (let [$editor ($ (find-plastic-editor editor-id))]
    (find $editor (str ".unit[data-pnid=" unit-id "]"))))

(defn find-react-mount-point [editor-id]
  (let [plastic-editor-view (find-plastic-editor-view editor-id)
        react-land-dom-nodes (.getElementsByClassName plastic-editor-view "react-land")]
    (assert react-land-dom-nodes)
    (assert (= (count react-land-dom-nodes) 1))
    (first react-land-dom-nodes)))