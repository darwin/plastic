(ns plastic.cogs.editor.layout.core
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [clojure.zip :as z]
            [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.cogs.editor.model :as editor]
            [plastic.cogs.editor.layout.builder :refer [build-layout]]
            [plastic.cogs.editor.layout.selections :refer [build-selections-render-info]]
            [plastic.cogs.editor.layout.structural :refer [build-structural-web]]
            [plastic.cogs.editor.layout.spatial :refer [build-spatial-web]]
            [plastic.util.zip :as zip-utils]
            [plastic.cogs.editor.layout.utils :as utils]
            [plastic.schema.paths :as paths]))

(defn prepare-form-layout-info [editor-id old-render-info root-loc]
  {:pre [(= (zip/tag (zip/up root-loc)) :forms)             ; parent has to be :forms
         (= 1 (count (node/children (zip/node (zip/up root-loc)))))]} ; root-loc is the only child
  (let [root-node (zip/node root-loc)
        _ (assert root-node)
        root-id (:id root-node)
        layout (build-layout (:render-data old-render-info) root-loc)
        selectables (utils/extract-all-selectables layout)
        spatial-web (build-spatial-web root-loc selectables)
        structural-web (build-structural-web :root root-loc)
        layout-info {:id             root-id                ; also known as form-id
                     :node           root-node              ; source node - used for optimization
                     :selectables    selectables            ; used for selections
                     :spatial-web    spatial-web            ; used for spatial left/right/up/down movement
                     :structural-web structural-web}]       ; used for structural left/right/up/down movement

    (log "LAYOUT: form #" root-id "=> render-info:" layout-info)
    (dispatch :editor-commit-layout editor-id root-id layout)
    layout-info))

(defn prepare-render-infos-of-top-level-forms [independent-top-level-locs editor]
  (let [prepare-item (fn [loc]
                       (let [node (z/node loc)
                             old-render-info (editor/get-render-info-by-id editor (:id node))]
                         (if (= (:node old-render-info) node) ; do not relayout form if not affected by changes
                           old-render-info
                           (prepare-form-layout-info (:id editor) old-render-info loc))))]
    (into {} (map (fn [loc] [(zip-utils/loc-id loc) (prepare-item loc)]) independent-top-level-locs))))

(defn layout-editor [editor]
  (if-not (editor/parsed? editor)
    editor
    (let [independent-top-level-locs (map zip/down (map zip-utils/independent-zipper (editor/get-top-level-locs editor)))
          render-state {:order             (map #(zip-utils/loc-id %) independent-top-level-locs)
                        :forms             (prepare-render-infos-of-top-level-forms independent-top-level-locs editor)
                        :debug-parse-tree  (editor/get-parse-tree editor)
                        :debug-text-input  (editor/get-input-text editor)
                        :debug-text-output (editor/get-output-text editor)}]
      (-> editor
        (editor/set-render-state render-state)))))

(defn update-layout [editors [editor-selector]]
  (editor/apply-to-specified-editors layout-editor editors editor-selector))

(defn commit-layout [editors [editor-id form-id layout]]
  (let [editor (get editors editor-id)
        new-editor (editor/set-layout-for-form editor form-id layout)]
    (assoc editors editor-id new-editor)))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-update-layout paths/editors-path update-layout)
(register-handler :editor-commit-layout paths/editors-path commit-layout)