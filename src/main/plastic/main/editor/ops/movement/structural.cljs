(ns plastic.main.editor.ops.movement.structural
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.frame :refer [dispatch]]
                   [plastic.common :refer [process]])
  (:require [plastic.main.editor.model :as editor]))

; -------------------------------------------------------------------------------------------------------------------

(defn structural-movemement [op editor]
  (let [cursor-id (editor/get-cursor editor)
        form-id (editor/get-focused-unit-id editor)
        structural-web (editor/get-structural-web-for-unit editor form-id)]
    (if-let [result-id (op (get structural-web cursor-id))]
      (editor/set-cursor editor result-id))))
