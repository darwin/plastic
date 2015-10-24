(ns plastic.main.editor.ops.movement.interest
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.editor.model :as editor]
            [plastic.util.helpers :as helpers]))

; -------------------------------------------------------------------------------------------------------------------

(defn is-token-of-interest? [token]
  (let [type (:type token)]
    (not (#{:linebreak :spot} type))))

(defn interest-movement [direction editor]
  (let [cursor-id (editor/get-cursor editor)
        form-id (editor/get-focused-unit-id editor)
        spatial-web (editor/get-spatial-web-for-unit editor form-id)
        all-lines (filter is-token-of-interest? (apply concat (vals spatial-web)))
        move (case direction
               :prev helpers/prev-item
               :next helpers/next-item)]
    (if-let [result (move #(= (:id %) cursor-id) all-lines)]
      (editor/set-cursor editor (:id result)))))
