(ns plastic.cogs.editor.layout.core
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! timeout]]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [clojure.zip :as z]
            [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.schema.paths :as paths]
            [plastic.cogs.editor.model :as editor]
            [plastic.cogs.editor.layout.analysis.calls :refer [analyze-calls]]
            [plastic.cogs.editor.layout.analysis.scopes :refer [analyze-scopes]]
            [plastic.cogs.editor.layout.analysis.defs :refer [analyze-defs]]
            [plastic.cogs.editor.layout.soup :refer [build-soup-render-info]]
            [plastic.cogs.editor.layout.code :refer [build-code-render-tree]]
            [plastic.cogs.editor.layout.docs :refer [build-docs-render-tree]]
            [plastic.cogs.editor.layout.headers :refer [build-headers-render-tree]]
            [plastic.cogs.editor.layout.selections :refer [build-selections-render-info]]
            [plastic.cogs.editor.layout.structural :refer [build-structural-web]]
            [plastic.cogs.editor.layout.spatial :refer [build-spatial-web]]
            [plastic.cogs.editor.analyzer :refer [analyze-full]]
            [plastic.cogs.editor.layout.utils :as layout-utils]
            [plastic.util.zip :as zip-utils]))

(defn reduce-render-tree [f val node]
  (f (reduce (partial reduce-render-tree f) val (:children node)) node))

(defn extract-selectables [res node]
  (if (:selectable? node)
    (conj res [(:id node) node])
    res))

(defn extract-all-selectables [render-tree]
  (reduce-render-tree extract-selectables {} render-tree))

(defn compose-render-trees [top-id headers docs code]
  {:tag         :tree
   :id          top-id
   :selectable? true
   :children    (remove nil? [headers docs code])})

(defn prepare-form-layout-info [settings root-loc]
  {:pre [(= (zip/tag (zip/up root-loc)) :forms)             ; parent has to be :forms
         (= 1 (count (node/children (zip/node (zip/up root-loc)))))]} ; root-loc is the only child
  (let [root-node (zip/node root-loc)
        _ (assert root-node)
        root-id (:id root-node)
        top-id (dec root-id)                                ; a hack - root-id was minimal, we rely ids to be assigned by parser in depth-first-order
        {:keys [code-visible docs-visible headers-visible]} settings
        code-render-tree (if code-visible (build-code-render-tree root-loc))
        docs-render-tree (if docs-visible (build-docs-render-tree root-loc))
        headers-render-tree (if headers-visible (build-headers-render-tree root-loc))
        render-tree (compose-render-trees top-id headers-render-tree docs-render-tree code-render-tree)
        selectables (extract-all-selectables render-tree)
        spatial-web (build-spatial-web selectables)
        structural-web (build-structural-web top-id selectables root-loc)
        layout-info {:id             root-id
                     :node           root-node
                     :selectables    selectables            ; used for selections
                     :spatial-web    spatial-web            ; used for spatial left/right/up/down movement
                     :structural-web structural-web         ; used for structural left/right/up/down movement
                     :render-tree    render-tree}]          ; used for skelet rendering
    (log "LAYOUT: form #" root-id "=> render-info:" layout-info)
    layout-info))

(defn prepare-render-infos-of-top-level-forms [independent-top-level-locs settings editor form-id]
  (let [prepare-item (fn [loc]
                       (let [node (z/node loc)
                             old-render-info (editor/get-render-info-by-id editor (:id node))]
                         (if (= (:node old-render-info) node) ; do not relayout form if not affected by changes
                           old-render-info
                           (prepare-form-layout-info settings loc))))]
    (into {} (map (fn [loc] [(zip-utils/loc-id loc) (prepare-item loc)]) independent-top-level-locs))))

(defn layout-editor [settings form-id editor]
  (if-not (editor/parsed? editor)
    editor
    (let [independent-top-level-locs (map zip/down (map zip-utils/independent-zipper (editor/get-top-level-locs editor)))
          render-state {:order (map #(zip-utils/loc-id %) independent-top-level-locs)
                        :forms (prepare-render-infos-of-top-level-forms independent-top-level-locs settings editor form-id)
                        :debug-parse-tree (editor/get-parse-tree editor)
                        :debug-text-input (editor/get-input-text editor)
                        :debug-text-output (editor/get-output-text editor)}]
      (-> editor
        (editor/set-render-state render-state)))))

(defn update-layout [db [editor-selector form-id]]
  (let [{:keys [editors settings]} db
        new-editors (layout-utils/apply-to-specified-editors (partial layout-editor settings form-id) editors editor-selector)]
    (assoc db :editors new-editors)))

(defn prepare-form-analysis [root-loc]
  {:pre [(= (node/tag (zip/node (zip/up root-loc))) :forms)]} ; parent has to be :forms
  (let [root-node (zip/node root-loc)
        _ (assert root-node)
        root-id (:id root-node)
        analysis (-> (sorted-map)                           ; just for debugging
                   (analyze-calls root-loc)
                   (analyze-scopes root-loc)
                   (analyze-defs root-loc))]
    (log "ANALYSIS: form #" root-id "=>" analysis)
    analysis))

(defn run-analysis-for-editor-and-form [editor opts form-loc]
  (log "running analysis for editor" (:id editor) "and form" (zip-utils/loc-id form-loc) "with opts" opts)
  (dispatch :editor-commit-analysis (:id editor) (zip-utils/loc-id form-loc) (prepare-form-analysis form-loc)))

(defn run-analysis-for-editor-and-forms [form-selector opts editor]
  (if (editor/parsed? editor)
    (editor/doall-specified-forms editor (partial run-analysis-for-editor-and-form editor opts) form-selector)
    (error "editor not parsed!" editor)))

(defn run-analysis [db [editor-selector form-selector opts]]
  (let [{:keys [editors]} db]
    (layout-utils/doall-specified-editors (partial run-analysis-for-editor-and-forms form-selector opts) editors editor-selector)
    db))

(defn commit-analysis [editors [editor-id form-id analysis]]
  (assoc-in editors [editor-id :analysis form-id] analysis))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-update-layout update-layout)
(register-handler :editor-run-analysis run-analysis)
(register-handler :editor-commit-analysis paths/editors-path commit-analysis)