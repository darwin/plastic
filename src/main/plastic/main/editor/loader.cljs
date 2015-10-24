(ns plastic.main.editor.loader
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.frame :refer [worker-dispatch]])
  (:require [plastic.onion.atom :as atom]
            [plastic.main.editor.model :as editor]))

; -------------------------------------------------------------------------------------------------------------------

(defn fetch-text [context db [editor-selector]]
  (editor/apply-to-editors context db editor-selector
    (fn [editor]
      (let [editor-id (editor/get-id editor)
            uri (editor/get-uri editor)]
        (atom/load-file-content uri #(worker-dispatch context [:editor-set-source editor-id %]))
        editor))))