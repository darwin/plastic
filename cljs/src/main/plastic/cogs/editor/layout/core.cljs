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
        layout-info {:id   root-id
                     :node root-node}]
    (dispatch :editor-commit-layout editor-id root-id layout)
    (dispatch :editor-commit-selectables editor-id root-id selectables)
    (dispatch :editor-commit-spatial-web editor-id root-id spatial-web)
    (dispatch :editor-commit-structural-web editor-id root-id structural-web)
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

(defn commit-selectables [editors [editor-id form-id selectables]]
  (let [editor (get editors editor-id)
        new-editor (editor/set-selectables-for-form editor form-id selectables)]
    (assoc editors editor-id new-editor)))

(defn commit-spatial-web [editors [editor-id form-id spatial-web]]
  (let [editor (get editors editor-id)
        new-editor (editor/set-spatial-web-for-form editor form-id spatial-web)]
    (assoc editors editor-id new-editor)))

(defn commit-structural-web [editors [editor-id form-id structural-web]]
  (let [editor (get editors editor-id)
        new-editor (editor/set-structural-web-for-form editor form-id structural-web)]
    (assoc editors editor-id new-editor)))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-update-layout paths/editors-path update-layout)
(register-handler :editor-commit-layout paths/editors-path commit-layout)
(register-handler :editor-commit-selectables paths/editors-path commit-selectables)
(register-handler :editor-commit-spatial-web paths/editors-path commit-spatial-web)
(register-handler :editor-commit-structural-web paths/editors-path commit-structural-web)