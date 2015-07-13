(ns plastic.cogs.editor.layout.core
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! timeout]]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [clojure.zip :as z]
            [plastic.util.helpers :as helpers]
            [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.schema.paths :as paths]
            [plastic.cogs.editor.model :as editor]
            [plastic.cogs.editor.layout.analysis.selectables :refer [analyze-selectables]]
            [plastic.cogs.editor.layout.analysis.scopes :refer [analyze-scopes]]
            [plastic.cogs.editor.layout.analysis.symbols :refer [analyze-symbols]]
            [plastic.cogs.editor.layout.analysis.editing :refer [analyze-editing]]
            [plastic.cogs.editor.layout.analysis.defs :refer [analyze-defs]]
            [plastic.cogs.editor.layout.soup :refer [build-soup-render-info]]
            [plastic.cogs.editor.layout.code :refer [build-code-render-tree]]
            [plastic.cogs.editor.layout.docs :refer [build-docs-render-tree]]
            [plastic.cogs.editor.layout.headers :refer [build-headers-render-tree]]
            [plastic.cogs.editor.layout.selections :refer [build-selections-render-info]]
            [plastic.cogs.editor.layout.structural :refer [build-structural-web]]
            [plastic.cogs.editor.layout.spatial :refer [build-spatial-web]]
            [plastic.cogs.editor.analyzer :refer [analyze-full]]
            [plastic.cogs.editor.layout.utils :as layout-utils]))

(defn reduce-render-tree [f val node]
  (f (reduce (partial reduce-render-tree f) val (:children node)) node))

(defn extract-tokens [res node]
  (if (= (:tag node) :token)
    (conj res [(:id node) node])
    res))

(defn extract-selectables [res node]
  (if (:selectable? node)
    (conj res [(:id node) node])
    res))

(defn collect-nodes [node]
  (let [relevant-nodes (fn [nodes] (remove #(or (node/whitespace? %) (node/comment? %)) nodes))
        child-nodes (fn [node]
                      (if (node/inner? node)
                        (flatten (map collect-nodes (relevant-nodes (node/children node))))))]
    (concat [(:id node) node] (child-nodes node))))

(defn extract-all-selectables [render-tree]
  (reduce-render-tree extract-selectables {} render-tree))

(defn compose-render-trees [top-id headers docs code]
  {:tag         :tree
   :id          top-id
   :selectable? true
   :children    (remove nil? [headers docs code])})

(defn prepare-form-render-info [settings editor loc]
  {:pre [(= (node/tag (zip/node (zip/up loc))) :forms)]}    ; parent has to be :forms
  (let [root-node (zip/node loc)
        _ (assert root-node)
        root-id (:id root-node)
        top-id (dec root-id)                                ; a hack - root-id was minimal, we rely ids to be assigned by parser in dept-first-order
        editing-set (editor/get-editing-set editor)
        nodes (apply hash-map (collect-nodes root-node))
        {:keys [code-visible docs-visible]} settings
        _ (log settings)

        analysis (->> {}
                   (analyze-selectables nodes)
                   (analyze-scopes root-node)
                   (analyze-symbols nodes)
                   (analyze-defs root-node)
                   (analyze-editing editing-set))

        code-render-tree (if code-visible (build-code-render-tree analysis loc))
        docs-render-tree (if docs-visible (build-docs-render-tree analysis nodes))
        headers-render-tree (build-headers-render-tree analysis loc)
        render-tree (compose-render-trees top-id headers-render-tree docs-render-tree code-render-tree)
        _ (log "AAAAA" render-tree)

        selectables (extract-all-selectables render-tree)

        spatial-web (build-spatial-web selectables)
        structural-web (build-structural-web top-id selectables loc)

        form-info {:id             root-id
                   :editing        (editor/editing? editor)
                   :nodes          nodes
                   :analysis       analysis
                   :selectables    selectables              ; used for selections
                   :spatial-web    spatial-web              ; used for spatial left/right/up/down movement
                   :structural-web structural-web           ; used for structural left/right/up/down movement
                   :render-tree    render-tree}]            ; used for skelet rendering
    ;(debug-print-analysis root-node nodes analysis)
    (log "form #" root-id "=> render-info:" form-info)
    form-info))

(defn prepare-render-infos-of-top-level-forms [settings editor form-id]
  (let [top-level-forms-locs (editor/get-top-level-locs editor)
        previous-render-info #(editor/get-render-info-by-id editor (:id (z/node %)))
        prepare-item #(merge
                       (previous-render-info %)
                       (prepare-form-render-info settings editor %))
        needs-update? #(or (nil? form-id) (= (:id (z/node %)) form-id))]
    (map #(if (needs-update? %)
           (prepare-item %)
           (previous-render-info %)) top-level-forms-locs)))

(defn analyze-with-delay [editor-id delay]
  (go
    (<! (timeout delay))                                    ; give it some time to render UI (next requestAnimationFrame)
    (dispatch :editor-analyze editor-id)))

(defn layout-editor [settings form-id editor]
  (if-not (editor/parsed? editor)
    editor
    (let [render-state {:forms             (prepare-render-infos-of-top-level-forms settings editor form-id)
                        :debug-parse-tree  (editor/get-parse-tree editor)
                        :debug-text-input  (editor/get-input-text editor)
                        :debug-text-output (editor/get-output-text editor)}]
      (editor/set-render-state editor render-state))))

(defn update-layout [db [editor-selector form-id]]
  (let [{:keys [editors settings]} db
        new-editors (layout-utils/apply-to-selected-editors (partial layout-editor settings form-id) editors editor-selector)]
    (assoc db :editors new-editors)))

(defn update-editor-layout-for-focused-form [settings editor]
  (layout-editor settings (editor/get-focused-form-id editor) editor))

(defn update-layout-for-focused-form [db [editor-selector]]
  (let [{:keys [editors settings]} db
        new-editors (layout-utils/apply-to-selected-editors (partial update-editor-layout-for-focused-form settings) editors editor-selector)]
    (assoc db :editors new-editors)))

(defn update-form-soup-geometry [geometry form]
  (let [tokens (reduce-render-tree extract-tokens {} (:render-tree form))]
    (assoc form :soup (build-soup-render-info tokens geometry))))

(defn update-soup-geometry [editors [editor-id form-id geometry]]
  (let [editor (get editors editor-id)
        old-forms (editor/get-render-infos editor)
        new-forms (helpers/update-selected #(= (:id %) form-id) (partial update-form-soup-geometry geometry) old-forms)
        new-editors (assoc editors editor-id (editor/set-render-infos editor new-forms))]
    new-editors))

(defn update-form-selectables-geometry [geometry form]
  (assoc form :all-selections (build-selections-render-info (:selectables form) geometry)))

(defonce debounced-selection-updaters ^:dynamic {})

(defn dispatch-editor-update-selections [editor-id]
  (dispatch :editor-update-selections editor-id))

(defn debounced-dispatch-editor-update-selections [editor-id]
  (if-let [updater (get debounced-selection-updaters editor-id)]
    (updater)
    (let [new-updater (helpers/debounce (partial dispatch-editor-update-selections editor-id) 30)]
      (set! debounced-selection-updaters (assoc debounced-selection-updaters editor-id new-updater))
      (new-updater))))

(defn update-selectables-geometry [editors [editor-id form-id geometry]]
  (let [editor (get editors editor-id)
        old-forms (editor/get-render-infos editor)
        new-forms (helpers/update-selected #(= (:id %) form-id) (partial update-form-selectables-geometry geometry) old-forms)
        new-editors (assoc editors editor-id (editor/set-render-infos editor new-forms))]
    (debounced-dispatch-editor-update-selections editor-id) ; coalesce update-requests here
    new-editors))

(defn update-form-selections [form selected-node-ids]
  {:pre [(or (nil? selected-node-ids) (set? selected-node-ids))]}
  (let [all-selections (:all-selections form)
        matching-selections (filter #(contains? selected-node-ids (first %)) all-selections)]
    (assoc form :active-selections (vec matching-selections))))

(defn update-form-focus-flag [form focused-form-id]
  (assoc form :focused (= (:id form) focused-form-id)))

(defn update-selections [editors [editor-id]]
  (let [editor (get editors editor-id)
        selections (editor/get-selections editor)
        get-form-selected-node-ids (fn [form] (get selections (:id form)))
        update-render-info (fn [form] (-> form
                                        (update-form-selections (get-form-selected-node-ids form))
                                        (update-form-focus-flag (:focused-form-id selections))))
        old-forms (editor/get-render-infos editor)
        new-forms (map update-render-info old-forms)
        new-editors (assoc editors editor-id (editor/set-render-infos editor new-forms))]
    new-editors))

(defn analyze [editors [editor-id]]
  (let [ast (analyze-full (get-in editors [editor-id :text]) {:atom-path (get-in editors [editor-id :def :uri])})]
    (log "AST>" ast))
  editors)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-update-layout update-layout)
(register-handler :editor-update-layout-for-focused-form update-layout-for-focused-form)
(register-handler :editor-update-soup-geometry paths/editors-path update-soup-geometry)
(register-handler :editor-update-selectables-geometry paths/editors-path update-selectables-geometry)
(register-handler :editor-update-selections paths/editors-path update-selections)
(register-handler :editor-analyze paths/editors-path analyze)
