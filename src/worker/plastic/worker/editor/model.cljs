(ns plastic.worker.editor.model
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.worker :refer [dispatch main-dispatch]]
                   [plastic.common :refer [process]])
  (:require [plastic.util.helpers :as helpers :refer [select-values]]
            [plastic.worker.editor.toolkit.id :as id]
            [meld.core :as meld]
            [meld.node :as node]
            [meld.zip :as zip]))

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

(def good? zip/good?)

(declare find-node-loc)

(defn valid-edit-point? [editor edit-point]
  (let [id (id/id-part edit-point)]
    (not (nil? (find-node-loc editor id)))))

; -------------------------------------------------------------------------------------------------------------------

(defn get-id [editor]
  {:post [(pos? %)]}
  (:id editor))

; -------------------------------------------------------------------------------------------------------------------

(defn get-source [editor]
  {:pre  [(valid-editor? editor)]
   :post [(string? %)]}
  (or (get editor :source) ""))

(defn set-source [editor source]
  {:pre [(valid-editor? editor)
         (string? source)]}
  (or
    (when-not (= (get-source editor) source)
      (dispatch :editor-parse-source (get-id editor))
      (assoc editor :source source))
    editor))

(defn get-uri [editor]
  {:pre  [(valid-editor? editor)]
   :post [(string? %)]}
  (or (get editor :uri) ""))

(defn set-uri [editor uri]
  {:pre [(valid-editor? editor)
         (string? uri)]}
  (or
    (when-not (= (get-uri editor) uri)
      (assoc editor :uri uri))
    editor))

; -------------------------------------------------------------------------------------------------------------------

(defn get-meld [editor]
  {:pre [(valid-editor? editor)]}
  (get editor :meld))

(defn set-meld [editor meld]
  {:pre [(valid-editor? editor)]}
  (if (identical? (get-meld editor) meld)
    editor
    (do
      (dispatch :editor-update-layout (get-id editor))
      (assoc editor :meld meld))))

(defn ^boolean has-meld? [editor]
  {:pre [(valid-editor? editor)]}
  (not (nil? (get-meld editor))))

; -------------------------------------------------------------------------------------------------------------------

(defn get-analysis-for-unit [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:analysis unit-id]))

(defn set-analysis-for-unit [editor unit-id new-analysis]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:analysis unit-id] new-analysis))

; -------------------------------------------------------------------------------------------------------------------

(defn get-layout-for-unit [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:layout unit-id]))

(defn set-layout-for-unit [editor unit-id new-layout]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:layout unit-id] new-layout))

; -------------------------------------------------------------------------------------------------------------------

(defn get-spatial-web-for-unit [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:spatial-web unit-id]))

(defn set-spatial-web-for-unit [editor unit-id new-spatial-web]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:spatial-web unit-id] new-spatial-web))

; -------------------------------------------------------------------------------------------------------------------

(defn get-structural-web-for-unit [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:structural-web unit-id]))

(defn set-structural-web-for-unit [editor unit-id new-structural-web]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:structural-web unit-id] new-structural-web))

; -------------------------------------------------------------------------------------------------------------------

(defn get-selectables-for-unit [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (let [layout (get-layout-for-unit editor unit-id)]
    (assert layout)
    (select-values :selectable? layout)))

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

(defn get-units [editor]
  {:pre [(valid-editor? editor)]}
  (get editor :units))

(defn set-units [editor units]
  {:pre [(valid-editor? editor)]}
  (if-not (identical? (get-units editor) units)
    (assoc editor :units units)
    editor))

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

(defn get-unit-locs [editor]
  (let [meld (get-meld editor)
        top-loc (zip/zip meld)]
    (zip/child-locs top-loc)))

(defn find-node-loc [editor node-id]
  {:pre [(valid-editor? editor)]}
  (let [meld (get-meld editor)
        top-loc (zip/zip meld)]
    (zip/find top-loc node-id)))

(defn find-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (let [node-loc (find-node-loc editor node-id)]
    (if (good? node-loc)
      (zip/get-node node-loc))))

(defn get-unit-ids [editor]
  {:pre [(valid-editor? editor)]}
  (let [meld (get-meld editor)
        top-node (meld/get-root-node meld)]
    (node/get-children top-node)))

; -------------------------------------------------------------------------------------------------------------------

(defn selector-matches-editor? [editor-id selector]
  {:pre [(valid-editor-id? editor-id)]}
  (cond
    (vector? selector) (some #{editor-id} selector)
    (set? selector) (contains? selector editor-id)
    :default (= editor-id selector)))

(defn apply-to-editors [editors selector f & args]
  (process (keys editors) editors
    (fn [editors editor-id]
      (or
        (if (selector-matches-editor? editor-id selector)
          (if-let [new-editor (apply f (get editors editor-id) args)]
            (assoc editors editor-id new-editor)))
        editors))))

(defn apply-to-units [editor selector f & args]
  {:pre [(valid-editor? editor)]}
  (process (get-unit-locs editor) editor
    (fn [editor unit-loc]
      (or (if (helpers/selector-matches? selector (zip/get-id unit-loc))
            (apply f editor unit-loc args))
        editor))))

; -------------------------------------------------------------------------------------------------------------------

(defn get-previously-layouted-unit-revision [editor unit-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:previously-layouted-units unit-id]))

(defn remember-previously-layouted-unit-revision [editor unit-id revision]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:previously-layouted-units unit-id] revision))

(defn prune-cache-of-previously-layouted-units [editor unit-ids-to-keep]
  {:pre [(valid-editor? editor)]}
  (let [form-nodes (get editor :previously-layouted-units)]
    (assoc editor :previously-layouted-units (select-keys form-nodes unit-ids-to-keep))))

; -------------------------------------------------------------------------------------------------------------------

(defn remove-unit [editor unit-id]
  {:pre [unit-id]}
  (-> editor
    (update :layout dissoc unit-id)
    (update :selectables dissoc unit-id)
    (update :spatial-web dissoc unit-id)
    (update :structural-web dissoc unit-id)
    (update :analysis dissoc unit-id)))

(defn remove-units [editor ids]
  {:pre [(valid-editor? editor)]}
  (process ids editor
    (fn [editor unit-id]
      (remove-unit editor unit-id))))

; -------------------------------------------------------------------------------------------------------------------

(defn get-node [editor id]
  {:pre [(valid-editor? editor)]}
  (let [meld (get-meld editor)]
    (meld/get-node meld id)))
