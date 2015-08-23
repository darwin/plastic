(ns plastic.main.editor.xform
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.frame :refer [register-handler]]
            [plastic.main.paths :as paths]
            [plastic.main.editor.model :as editor]))

; -------------------------------------------------------------------------------------------------------------------

(defn announce-xform [editors [selector report]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (editor/set-xform-report editor report))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-announce-xform paths/editors-path announce-xform)
