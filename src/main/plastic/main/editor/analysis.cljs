(ns plastic.main.editor.analysis
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.main.editor.model :as editor]))

; -------------------------------------------------------------------------------------------------------------------

(defn commit-analysis-patch [context db  [editor-selector form-id analysis-patch]]
  (editor/apply-to-editors context db editor-selector
    (fn [editor]
      (editor/set-analysis-patch-for-unit editor form-id analysis-patch))))

