(ns plastic.main.editor.analysis
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.paths :as paths]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.model :as editor]))

(defn commit-analysis-patch [editors [editor-selector form-id analysis-patch]]
  (editor/apply-to-editors editors editor-selector
    (fn [editor]
      (editor/set-analysis-patch-for-form editor form-id analysis-patch))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-commit-analysis-patch paths/editors-path commit-analysis-patch)
