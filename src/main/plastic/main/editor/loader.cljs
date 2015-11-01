(ns plastic.main.editor.loader
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.frame :refer-macros [worker-dispatch]]
            [plastic.onion.host :as host]
            [plastic.main.editor.model :as editor]))

; -------------------------------------------------------------------------------------------------------------------

(defn fetch-text [context db [editor-selector]]
  (editor/apply-to-editors context db editor-selector
    (fn [editor]
      (let [editor-id (editor/get-id editor)
            uri (editor/get-uri editor)]
        (host/load-file-content context uri #(worker-dispatch context [:editor-set-source editor-id %]))
        editor))))