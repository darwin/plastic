(ns plastic.main.editor.ops.movement.form
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch]]
                   [plastic.common :refer [process]])
  (:require [plastic.main.editor.model :as editor]))

(defn move-form [editor direction-fn next-selection-fn]
  (let [focused-form-id (editor/get-focused-form-id editor)
        top-level-ids (editor/get-top-level-form-ids editor)
        next-focused-form-id (direction-fn #(= % focused-form-id) top-level-ids)]
    (if next-focused-form-id
      (let [next-selection (next-selection-fn editor next-focused-form-id)]
        (editor/set-cursor editor next-selection)))))
