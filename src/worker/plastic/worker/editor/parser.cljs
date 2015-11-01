(ns plastic.worker.editor.parser
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.worker.editor.model :as editor]
            [meld.parser :as meld]))

; -------------------------------------------------------------------------------------------------------------------

(defn parse-source [context db [editor-selector]]
  (editor/apply-to-editors context db editor-selector
    (fn [editor]
      (let [source (editor/get-source editor)
            uri (editor/get-uri editor)
            meld (meld/parse! source uri)]
        (editor/set-meld editor meld)))))

