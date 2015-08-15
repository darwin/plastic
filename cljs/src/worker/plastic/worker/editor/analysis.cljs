(ns plastic.worker.editor.analysis
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.worker.glue :refer [react! dispatch main-dispatch]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.editor.model :as editor]
            [plastic.worker.editor.layout.analysis.calls :refer [analyze-calls]]
            [plastic.worker.editor.layout.analysis.scopes :refer [analyze-scopes]]
            [plastic.worker.editor.layout.analysis.defs :refer [analyze-defs]]
            [plastic.util.zip :as zip-utils]))

(defn prepare-form-analysis [root-loc _opts]
  {:pre [(= (node/tag (zip/node (zip/up root-loc))) :forms)]} ; parent has to be :forms
  (let [root-node (zip/node root-loc)
        _ (assert root-node)
        root-id (:id root-node)
        analysis (-> {}
                   (analyze-calls root-loc)
                   (analyze-scopes root-loc)
                   (analyze-defs root-loc))]
    (fancy-log "ANALYSIS" "form #" root-id "=>" analysis)
    analysis))

(defn run-analysis-for-editor-and-form [editor opts form-loc]
  (main-dispatch :editor-commit-analysis (editor/get-id editor) (zip-utils/loc-id form-loc) (prepare-form-analysis form-loc opts))
  #_(let [old-analysis nil #_(editor/get-analysis-for-form editor (zip-utils/loc-id form-loc))] ; TODO: optimize
    (if-not (= (:node old-analysis) (z/node form-loc))      ; skip analysis if nothing changed
      (main-dispatch :editor-commit-analysis (editor/get-id editor) (zip-utils/loc-id form-loc) (prepare-form-analysis form-loc opts)))))

(defn run-analysis-for-editor-and-forms [form-selector opts editor]
  (if (editor/parsed? editor)
    (editor/doall-specified-forms editor (partial run-analysis-for-editor-and-form editor opts) form-selector)
    (error "editor not parsed!" editor)))

(defn run-analysis [db [editor-selector form-selector opts]]
  (let [{:keys [editors]} db]
    (editor/doall-specified-editors (partial run-analysis-for-editor-and-forms form-selector opts) editors editor-selector)
    db))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-run-analysis run-analysis)