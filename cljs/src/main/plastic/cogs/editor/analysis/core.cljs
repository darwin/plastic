(ns plastic.cogs.editor.analysis.core
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.core.async :refer [<! timeout]]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.schema.paths :as paths]
            [plastic.cogs.editor.model :as editor]
            [plastic.cogs.editor.layout.analysis.calls :refer [analyze-calls]]
            [plastic.cogs.editor.layout.analysis.scopes :refer [analyze-scopes]]
            [plastic.cogs.editor.layout.analysis.defs :refer [analyze-defs]]
            [plastic.util.zip :as zip-utils]))

(defn prepare-form-analysis [root-loc _opts]
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
  (dispatch :editor-commit-analysis (:id editor) (zip-utils/loc-id form-loc) (prepare-form-analysis form-loc opts)))

(defn run-analysis-for-editor-and-forms [form-selector opts editor]
  (if (editor/parsed? editor)
    (editor/doall-specified-forms editor (partial run-analysis-for-editor-and-form editor opts) form-selector)
    (error "editor not parsed!" editor)))

(defn run-analysis [db [editor-selector form-selector opts]]
  (let [{:keys [editors]} db]
    (editor/doall-specified-editors (partial run-analysis-for-editor-and-forms form-selector opts) editors editor-selector)
    db))

(defn commit-analysis [editors [editor-id form-id analysis]]
  (assoc-in editors [editor-id :analysis form-id] analysis))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-run-analysis run-analysis)
(register-handler :editor-commit-analysis paths/editors-path commit-analysis)