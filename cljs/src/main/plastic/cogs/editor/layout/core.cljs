(ns plastic.cogs.editor.layout.core
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [clojure.zip :as z]
            [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.cogs.editor.model :as editor]
            [plastic.cogs.editor.layout.code :refer [build-code-render-tree]]
            [plastic.cogs.editor.layout.docs :refer [build-docs-render-tree]]
            [plastic.cogs.editor.layout.headers :refer [build-headers-render-tree]]
            [plastic.cogs.editor.layout.selections :refer [build-selections-render-info]]
            [plastic.cogs.editor.layout.structural :refer [build-structural-web]]
            [plastic.cogs.editor.layout.spatial :refer [build-spatial-web]]
            [plastic.util.zip :as zip-utils]
            [plastic.cogs.editor.layout.utils :as utils]
            [plastic.cogs.editor.parser.utils :as parser]))

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
        top-id (parser/next-node-id!)
        {:keys [code-visible docs-visible headers-visible]} settings
        code-render-tree (if code-visible (build-code-render-tree root-loc))
        docs-render-tree (if docs-visible (build-docs-render-tree root-loc))
        headers-render-tree (if headers-visible (build-headers-render-tree root-loc))
        render-tree (compose-render-trees top-id headers-render-tree docs-render-tree code-render-tree) ; TODO: we should build all trees in one go
        selectables (utils/extract-all-selectables render-tree)
        spatial-web (build-spatial-web render-tree)
        structural-web (build-structural-web top-id selectables root-loc)
        layout-info {:id             root-id                ; also known as form-id
                     :node           root-node              ; source node - used for optimization
                     :selectables    selectables            ; used for selections
                     :spatial-web    spatial-web            ; used for spatial left/right/up/down movement
                     :structural-web structural-web         ; used for structural left/right/up/down movement
                     :render-tree    render-tree}]          ; used for skelet rendering
    (log "LAYOUT: form #" root-id "=> render-info:" layout-info)
    layout-info))

(defn prepare-render-infos-of-top-level-forms [independent-top-level-locs settings editor]
  (let [prepare-item (fn [loc]
                       (let [node (z/node loc)
                             old-render-info (editor/get-render-info-by-id editor (:id node))]
                         (if (= (:node old-render-info) node) ; do not relayout form if not affected by changes
                           old-render-info
                           (prepare-form-layout-info settings loc))))]
    (into {} (map (fn [loc] [(zip-utils/loc-id loc) (prepare-item loc)]) independent-top-level-locs))))

(defn layout-editor [settings editor]
  (if-not (editor/parsed? editor)
    editor
    (let [independent-top-level-locs (map zip/down (map zip-utils/independent-zipper (editor/get-top-level-locs editor)))
          render-state {:order             (map #(zip-utils/loc-id %) independent-top-level-locs)
                        :forms             (prepare-render-infos-of-top-level-forms independent-top-level-locs settings editor)
                        :debug-parse-tree  (editor/get-parse-tree editor)
                        :debug-text-input  (editor/get-input-text editor)
                        :debug-text-output (editor/get-output-text editor)}]
      (-> editor
        (editor/set-render-state render-state)))))

(defn update-layout [db [editor-selector]]
  (let [{:keys [editors settings]} db
        new-editors (editor/apply-to-specified-editors (partial layout-editor settings) editors editor-selector)]
    (assoc db :editors new-editors)))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-update-layout update-layout)