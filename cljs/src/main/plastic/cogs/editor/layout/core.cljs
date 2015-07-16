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
            [plastic.cogs.editor.layout.utils :as layout-utils]
            [plastic.cogs.editor.render.dom :as dom]))

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

(defn prepare-form-render-info [settings editor root-loc]
  {:pre [(= (node/tag (zip/node (zip/up root-loc))) :forms)]} ; parent has to be :forms
  (let [root-node (zip/node root-loc)
        _ (assert root-node)
        root-id (:id root-node)
        top-id (dec root-id)                                ; a hack - root-id was minimal, we rely ids to be assigned by parser in dept-first-order
        nodes (apply hash-map (collect-nodes root-node))
        {:keys [code-visible docs-visible headers-visible]} settings

        analysis (->> {}
                   (analyze-selectables nodes)
                   (analyze-scopes root-loc)
                   (analyze-symbols nodes)
                   (analyze-defs root-node))

        code-render-tree (if code-visible (build-code-render-tree analysis root-loc))
        docs-render-tree (if docs-visible (build-docs-render-tree analysis nodes))
        headers-render-tree (if headers-visible (build-headers-render-tree analysis root-loc))
        render-tree (compose-render-trees top-id headers-render-tree docs-render-tree code-render-tree)

        selectables (extract-all-selectables render-tree)

        spatial-web (build-spatial-web selectables)
        structural-web (build-structural-web top-id selectables root-loc)

        form-info {:id             root-id
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
                       (prepare-form-render-info settings editor %))]
    (into {} (map (fn [loc] [(:id (z/node loc)) (prepare-item loc)]) top-level-forms-locs))))

(defn layout-editor [settings form-id editor]
  (if-not (editor/parsed? editor)
    editor
    (let [render-state {:forms             (prepare-render-infos-of-top-level-forms settings editor form-id)
                        :debug-parse-tree  (editor/get-parse-tree editor)
                        :debug-text-input  (editor/get-input-text editor)
                        :debug-text-output (editor/get-output-text editor)}]
      (-> editor
        (editor/set-render-state render-state)))))

(defn update-layout [db [editor-selector form-id]]
  (let [{:keys [editors settings]} db
        new-editors (layout-utils/apply-to-selected-editors (partial layout-editor settings form-id) editors editor-selector)]
    (assoc db :editors new-editors)))
; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-update-layout update-layout)