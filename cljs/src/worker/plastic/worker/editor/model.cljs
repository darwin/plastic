(ns plastic.worker.editor.model
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.findz :as findz]
            [rewrite-clj.node :as node]
            [plastic.util.helpers :as helpers]
            [plastic.util.zip :as zip-utils]))

(defprotocol IEditor)

(defrecord Editor [id def]
  IEditor)

(defn make [editor-id editor-def]
  (Editor. editor-id editor-def))

(defn valid-editor? [editor]
  (satisfies? IEditor editor))

(defn valid-editor-id? [editor-id]
  (pos? editor-id))

(def valid-loc? zip-utils/valid-loc?)

; -------------------------------------------------------------------------------------------------------------------

(defn get-id [editor]
  {:post [(pos? %)]}
  (:id editor))

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

(defn get-parse-tree-zipper [editor]
  (let [parse-tree (get-parse-tree editor)]
    (zip-utils/make-zipper parse-tree)))                                                                              ; root "forms" node

(defn get-top-level-locs [editor]
  (let [first-top-level-form-loc (zip/down (get-parse-tree-zipper editor))]
    (zip-utils/collect-all-right first-top-level-form-loc)))                                                          ; TODO: here we should use explicit zipping policy

(defn find-node-loc [editor node-id]
  {:pre [(valid-editor? editor)]}
  (findz/find-depth-first (get-parse-tree-zipper editor) (partial zip-utils/loc-id? node-id)))

(defn find-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (let [node-loc (find-node-loc editor node-id)]
    (if (valid-loc? node-loc)
      (zip/node node-loc))))

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
