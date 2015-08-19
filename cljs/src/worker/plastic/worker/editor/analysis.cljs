(ns plastic.worker.editor.analysis
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.worker :refer [react! dispatch main-dispatch]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [plastic.worker.servant]
            [plastic.worker.frame :refer [subscribe register-handler]]
            [plastic.worker.editor.model :as editor]
            [plastic.worker.editor.layout.analysis.calls :refer [analyze-calls]]
            [plastic.worker.editor.layout.analysis.scopes :refer [analyze-scopes]]
            [plastic.worker.editor.layout.analysis.defs :refer [analyze-defs]]
            [plastic.util.zip :as zip-utils]
            [plastic.worker.paths :as paths]))

; TODO: implement a cache to prevent recomputing analysis for unchanged forms
(defn prepare-form-analysis [form-loc]
  {:pre [(= (node/tag (zip/node (zip/up form-loc))) :forms)]} ; parent has to be :forms
  (let [form-node (zip/node form-loc)
        _ (assert form-node)
        root-id (:id form-node)
        analysis (-> {}
                   (analyze-calls form-loc)
                   (analyze-scopes form-loc)
                   (analyze-defs form-loc))]
    (fancy-log "ANALYSIS" "form" root-id "=>" analysis)
    analysis))

(defn run-analysis [editors [editor-selector form-selector]]
  (editor/apply-to-editors editors editor-selector
    (fn [editor]
      (if-not (editor/parsed? editor)
        (error "editor not parsed!" editor)
        (editor/apply-to-forms editor form-selector
          (fn [editor form-loc]
            (main-dispatch :editor-commit-analysis (editor/get-id editor) (zip-utils/loc-id form-loc) (prepare-form-analysis form-loc))))))))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-run-analysis paths/editors-path run-analysis)