(ns plastic.main.editor.analysis
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch]])
  (:require [plastic.main.frame :refer [subscribe register-handler]]
            [plastic.main.schema.paths :as paths]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.model :as editor]))

(defn commit-analysis [editors [editor-id form-id analysis]]
  (let [editor (get editors editor-id)
        new-editor (editor/set-analysis-for-form editor form-id analysis)]
    (assoc editors editor-id new-editor)))

; ----------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-commit-analysis paths/editors-path commit-analysis)