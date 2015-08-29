(ns plastic.worker.editor.model
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.worker :refer [dispatch main-dispatch]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.findz :as findz]
            [rewrite-clj.node :as node]
            [plastic.util.helpers :as helpers]
            [plastic.util.zip :as zip-utils]
            [plastic.worker.editor.toolkit.id :as id]
            [clojure.zip :as z]))

(defprotocol IEditor)

(defrecord Editor [id uri]
  IEditor)

(extend-protocol IHash
  Editor
  (-hash [this] (goog/getUid this)))

(defn make [editor-id editor-uri]
  (Editor. editor-id editor-uri))

(defn valid-editor? [editor]
  (satisfies? IEditor editor))

(defn valid-editor-id? [editor-id]
  (pos? editor-id))

(def valid-loc? zip-utils/valid-loc?)

(declare find-node-loc)

(defn valid-edit-point? [editor edit-point]
  (let [id (id/id-part edit-point)]
    (not (nil? (find-node-loc editor id)))))

; -------------------------------------------------------------------------------------------------------------------

(defn get-id [editor]
  {:post [(pos? %)]}
  (:id editor))

; -------------------------------------------------------------------------------------------------------------------

(defn get-text [editor]
  {:pre  [(valid-editor? editor)]
   :post [(string? %)]}
  (or (get editor :text) ""))

(defn set-text [editor text]
  {:pre [(valid-editor? editor)
         (string? text)]}
  (or
    (when-not (= (get-text editor) text)
      (dispatch :editor-parse-source (get-id editor))
      (assoc editor :text text))
    editor))

; -------------------------------------------------------------------------------------------------------------------

(defn get-parse-tree [editor]
  {:pre [(valid-editor? editor)]}
  (get editor :parse-tree))

(defn set-parse-tree [editor parse-tree]
  {:pre [(valid-editor? editor)
         (= (node/tag parse-tree) :forms)]}
  (or
    (when-not (identical? (get-parse-tree editor) parse-tree)
      (dispatch :editor-update-layout (get-id editor))
      (assoc editor :parse-tree parse-tree))
    editor))

(defn parsed? [editor]
  {:pre [(valid-editor? editor)]}
  (not (nil? (get-parse-tree editor))))

; -------------------------------------------------------------------------------------------------------------------

(defn get-analysis-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:analysis form-id]))

(defn set-analysis-for-form [editor form-id new-analysis]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:analysis form-id] new-analysis))

; -------------------------------------------------------------------------------------------------------------------

(defn get-layout-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:layout form-id]))

(defn set-layout-for-form [editor form-id new-layout]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:layout form-id] new-layout))

; -------------------------------------------------------------------------------------------------------------------

(defn get-selectables-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:selectables form-id]))

(defn set-selectables-for-form [editor form-id new-selectables]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:selectables form-id] new-selectables))

; -------------------------------------------------------------------------------------------------------------------

(defn get-spatial-web-for-form [editor form-id]
  {:pre  [(valid-editor? editor)]
   :post [(or (nil? %) (instance? PersistentTreeMap %))]}                                                             ; spatial-web must be sorted by line numbers
  (get-in editor [:spatial-web form-id]))

(defn set-spatial-web-for-form [editor form-id new-spatial-web]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:spatial-web form-id] new-spatial-web))

; -------------------------------------------------------------------------------------------------------------------

(defn get-structural-web-for-form [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:structural-web form-id]))

(defn set-structural-web-for-form [editor form-id new-structural-web]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:structural-web form-id] new-structural-web))

; -------------------------------------------------------------------------------------------------------------------

(defn get-xform-report [editor]
  {:pre [(valid-editor? editor)]}
  (let [report (get editor :xform-report)]
    (assert report)
    report))

(defn set-xform-report [editor report]
  {:pre [(valid-editor? editor)]}
  (assoc editor :xform-report report))

(defn remove-xform-report [editor]
  {:pre [(valid-editor? editor)]}
  (dissoc editor :xform-report))

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
  (let [root-loc (get-parse-tree-zipper editor)]
    (zip-utils/find #(if (zip-utils/loc-id? node-id %) %) root-loc)))

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
