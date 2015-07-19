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

(defn update-form-layout [editor-id form-loc]
  {:pre [(zip/node form-loc)
         (= (zip/tag (zip/up form-loc)) :forms)             ; parent has to be :forms
         (= 1 (count (node/children (zip/node (zip/up form-loc)))))]} ; root-loc is the only child
  (let [form-id (zip-utils/loc-id form-loc)
        layout (build-layout form-loc)
        selectables (utils/extract-all-selectables layout)
        spatial-web (build-spatial-web form-loc selectables)
        structural-web (build-structural-web :root form-loc)]
    (dispatch :editor-commit-layout editor-id form-id layout)
    (dispatch :editor-commit-selectables editor-id form-id selectables)
    (dispatch :editor-commit-spatial-web editor-id form-id spatial-web)
    (dispatch :editor-commit-structural-web editor-id form-id structural-web)))

(defn update-forms-layout-when-needed [independent-top-level-locs editor]
  (doseq [form-loc independent-top-level-locs]
    (let [form-node (z/node form-loc)
          form-id (:id form-node)
          cached-node (editor/get-cached-form-node editor form-id)]
      (when (not= cached-node form-node)
        (editor/set-cached-form-node editor form-node)
        (update-form-layout (:id editor) form-loc)))))

(defn layout-editor [editor]
  (if-not (editor/parsed? editor)
    editor
    (let [independent-top-level-locs (map zip/down (map zip-utils/independent-zipper (editor/get-top-level-locs editor)))
          _ (update-forms-layout-when-needed independent-top-level-locs editor)
          render-state {:order (map #(zip-utils/loc-id %) independent-top-level-locs)}]
      (editor/set-render-state editor render-state))))

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