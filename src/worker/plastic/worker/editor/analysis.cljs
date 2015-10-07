(ns plastic.worker.editor.analysis
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.worker :refer [dispatch main-dispatch]])
  (:require [plastic.worker.frame :refer [register-handler]]
            [plastic.worker.editor.model :as editor]
            [plastic.worker.editor.layout.analysis.calls :refer [analyze-calls]]
            [plastic.worker.editor.layout.analysis.scopes :refer [analyze-scopes]]
            [plastic.worker.editor.layout.analysis.defs :refer [analyze-defs]]
            [plastic.worker.paths :as paths]
            [plastic.util.helpers :as helpers]
            [meld.zip :as zip]))

; TODO: implement a cache to prevent recomputing analysis for unchanged forms
(defn prepare-unit-analysis [unit-loc]
  {:pre [(zip/unit? unit-loc)]}
  (-> {}
    (analyze-calls unit-loc)
    (analyze-scopes unit-loc)
    (analyze-defs unit-loc)
    (helpers/remove-nil-values)))

(defn run-analysis [editors [editor-selector form-selector]]
  (editor/apply-to-editors editors editor-selector
    (fn [editor]
      (if-not (editor/has-meld? editor)
        (error "editor not parsed! (no meld)" editor)
        (editor/apply-to-units editor form-selector
          (fn [editor unit-loc]
            (let [editor-id (editor/get-id editor)
                  unit-loc (zip/subzip unit-loc)
                  unit-id (zip/get-id unit-loc)
                  old-analysis (editor/get-analysis-for-unit editor unit-id)
                  new-analysis (prepare-unit-analysis unit-loc)
                  patch (helpers/prepare-map-patch old-analysis new-analysis)]
              (main-dispatch :editor-commit-analysis-patch editor-id unit-id patch)
              (editor/set-analysis-for-unit editor unit-id new-analysis))))))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-run-analysis paths/editors-path run-analysis)
