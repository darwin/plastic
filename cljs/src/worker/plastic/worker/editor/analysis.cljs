(ns plastic.worker.editor.analysis
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.worker :refer [react! dispatch main-dispatch]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [plastic.worker.frame :refer [register-handler]]
            [plastic.worker.editor.model :as editor]
            [plastic.worker.editor.layout.analysis.calls :refer [analyze-calls]]
            [plastic.worker.editor.layout.analysis.scopes :refer [analyze-scopes]]
            [plastic.worker.editor.layout.analysis.defs :refer [analyze-defs]]
            [plastic.util.zip :as zip-utils]
            [plastic.worker.paths :as paths]
            [plastic.util.helpers :as helpers]))

; TODO: implement a cache to prevent recomputing analysis for unchanged forms
(defn prepare-form-analysis [form-loc]
  {:pre [(zip-utils/valid-loc? form-loc)
         (= (node/tag (zip/node (zip/up form-loc))) :forms)]}                                                         ; parent has to be :forms
  (-> {}
    (analyze-calls form-loc)
    (analyze-scopes form-loc)
    (analyze-defs form-loc)
    (helpers/remove-nil-values)))

(defn run-analysis [editors [editor-selector form-selector]]
  (editor/apply-to-editors editors editor-selector
    (fn [editor]
      (if-not (editor/parsed? editor)
        (error "editor not parsed!" editor)
        (editor/apply-to-forms editor form-selector
          (fn [editor form-loc]
            (let [editor-id (editor/get-id editor)
                  form-id (zip-utils/loc-id form-loc)
                  analysis (prepare-form-analysis form-loc)]
              (main-dispatch :editor-commit-analysis editor-id form-id analysis))))))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-run-analysis paths/editors-path run-analysis)
