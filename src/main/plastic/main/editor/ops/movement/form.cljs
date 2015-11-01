(ns plastic.main.editor.ops.movement.form
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.ops.movement.spatial :refer [spatial-form-movement]]
            [plastic.util.helpers :as helpers]))

; -------------------------------------------------------------------------------------------------------------------

(defn form-movement [direction editor]
  (let [focused-form-id (editor/get-focused-unit-id editor)
        cursor-id (editor/get-cursor editor)
        dir (case direction
              :next :down
              :prev :up)]
    (spatial-form-movement editor focused-form-id cursor-id dir)))
