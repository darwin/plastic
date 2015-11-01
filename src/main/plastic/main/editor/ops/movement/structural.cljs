(ns plastic.main.editor.ops.movement.structural
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.common :refer-macros [process]]
            [plastic.main.editor.model :as editor]))

; -------------------------------------------------------------------------------------------------------------------

(defn structural-movemement [op editor]
  (let [cursor-id (editor/get-cursor editor)
        form-id (editor/get-focused-unit-id editor)
        structural-web (editor/get-structural-web-for-unit editor form-id)]
    (if-let [result-id (op (get structural-web cursor-id))]
      (editor/set-cursor editor result-id))))
