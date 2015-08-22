(ns plastic.worker.editor.model
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.findz :as findz]
            [rewrite-clj.node :as node]
            [plastic.worker.editor.parser.utils :as parser]
            [plastic.util.helpers :as helpers]
            [plastic.worker.editor.layout.builder :as layout]
            [plastic.util.zip :as zip-utils]
            [rewrite-clj.node.whitespace :refer [whitespace-node]]
            [rewrite-clj.node.token :refer [token-node]]
            [rewrite-clj.node.keyword :refer [keyword-node]]
            [rewrite-clj.node.whitespace :refer [newline-node]]
            [rewrite-clj.node.meta :refer [meta-node]]
            [rewrite-clj.node.fn :refer [fn-node]]
            [rewrite-clj.node.seq :refer [list-node vector-node map-node set-node]]
            [rewrite-clj.node.reader-macro :refer [deref-node]]
            [rewrite-clj.node.quote :refer [quote-node]]
            [reagent.ratom :refer [IDisposable dispose!]]
            [clojure.zip :as z]
            [plastic.worker.editor.model.zipping :as zipping]
            [plastic.worker.editor.toolkit.id :as id]))

; also see 'xforms' folder for more high-level editor transformations

(defprotocol IEditor)

(defrecord Editor [id def]
  IEditor)

(defn make [editor-id editor-def]
  (Editor. editor-id editor-def))

(defn valid-editor? [editor]
  (satisfies? IEditor editor))

(defn valid-editor-id? [editor-id]
  (pos? editor-id))

; -------------------------------------------------------------------------------------------------------------------

(defn get-id [editor]
  {:post [(pos? %)]}
  (:id editor))

; -------------------------------------------------------------------------------------------------------------------

; note comments are treated as line-breaks because they have new-lines embedded
(defn strip-whitespaces-but-keep-linebreaks-policy [loc]
  (let [node (z/node loc)]
    (and (or (node/linebreak? node) (not (node/whitespace? node))))))

(def zip-down (partial zip-utils/zip-down strip-whitespaces-but-keep-linebreaks-policy))
(def zip-right (partial zip-utils/zip-right strip-whitespaces-but-keep-linebreaks-policy))
(def zip-left (partial zip-utils/zip-left strip-whitespaces-but-keep-linebreaks-policy))

; -------------------------------------------------------------------------------------------------------------------

(defn get-parse-tree [editor]
  {:pre [(valid-editor? editor)]}
  (let [parse-tree (get editor :parse-tree)]
    (assert parse-tree)
    parse-tree))

(defn set-parse-tree [editor parse-tree]
  {:pre [(valid-editor? editor)
         (= (node/tag parse-tree) :forms)]}
  (assoc editor :parse-tree parse-tree))

(defn parsed? [editor]
  {:pre [(valid-editor? editor)]}
  (contains? editor :parse-tree))

; -------------------------------------------------------------------------------------------------------------------

(defn get-render-state [editor]
  {:pre [(valid-editor? editor)]}
  (get editor :render-state))

(defn set-render-state [editor render-state]
  {:pre [(valid-editor? editor)]}
  (let [old-render-state (get-render-state editor)]
    (if (not= old-render-state render-state)
      (assoc-in editor [:render-state] render-state)
      editor)))

; -------------------------------------------------------------------------------------------------------------------

(defn set-editing [editor node-id]
  {:pre [(valid-editor? editor)]}
  (if node-id
    (assoc editor :editing #{node-id})
    (assoc editor :editing #{})))

(defn editing? [editor]
  {:pre [(valid-editor? editor)]}
  (let [editing (:editing editor)]
    (and editing (not (empty? editing)))))

; -------------------------------------------------------------------------------------------------------------------

(defn get-top-level-locs [editor]
  {:pre [(valid-editor? editor)]}
  (let [top-loc (zip-utils/make-zipper (get-parse-tree editor))                                                       ; root "forms" node
        first-top-level-form-loc (zip/down top-loc)]
    (zip-utils/collect-all-right first-top-level-form-loc)))                                                          ; TODO: here we should use explicit zipping policy

(defn find-node-loc [editor node-id]
  {:pre [(valid-editor? editor)]}
  (let [parse-tree (get-parse-tree editor)
        root-loc (zip-utils/make-zipper parse-tree)
        node-loc (findz/find-depth-first root-loc (partial zip-utils/loc-id? node-id))]
    node-loc))

(defn find-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (let [node-loc (find-node-loc editor node-id)]
    (assert (zip-utils/valid-loc? node-loc))
    (zip/node node-loc)))

; -------------------------------------------------------------------------------------------------------------------

(defn selector-matches-editor? [editor-id selector]
  {:pre [(valid-editor-id? editor-id)]}
  (cond
    (vector? selector) (some #{editor-id} selector)
    (set? selector) (contains? selector editor-id)
    :default (= editor-id selector)))

(defn apply-to-editors [editors selector f]
  (let [reducer (fn [editors editor-id]
                  (or
                    (if (selector-matches-editor? editor-id selector)
                      (if-let [new-editor (f (get editors editor-id))]
                        (assoc editors editor-id new-editor)))
                    editors))]
    (reduce reducer editors (keys editors))))

(defn apply-to-forms [editor selector f]
  {:pre [(valid-editor? editor)]}
  (let [reducer (fn [editor form-loc]
                  (if (helpers/selector-matches? selector (zip-utils/loc-id form-loc))
                    (or (f editor form-loc) editor)
                    editor))]
    (reduce reducer editor (get-top-level-locs editor))))

; -------------------------------------------------------------------------------------------------------------------

(defn get-previously-layouted-form-node [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:previously-layouted-forms form-id]))

(defn remember-previously-layouted-form-node [editor form-node]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:previously-layouted-forms (:id form-node)] form-node))

(defn prune-cache-of-previously-layouted-forms [editor form-ids-to-keep]
  {:pre [(valid-editor? editor)]}
  (let [form-nodes (get editor :previously-layouted-forms)]
    (assoc editor :previously-layouted-forms (select-keys form-nodes form-ids-to-keep))))
