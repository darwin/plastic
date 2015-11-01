(ns plastic.main.editor.xform
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.main.editor.model :as editor]))

; -------------------------------------------------------------------------------------------------------------------

(defn announce-xform [context db [selector report]]
  (editor/apply-to-editors context db selector
    (fn [editor]
      (editor/set-xform-report editor report))))

