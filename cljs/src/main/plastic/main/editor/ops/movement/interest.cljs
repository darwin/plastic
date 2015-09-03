(ns plastic.main.editor.ops.movement.interest
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.main :refer [react! dispatch]]
                   [plastic.common :refer [process]])
  (:require [plastic.main.editor.model :as editor]))

(defn is-token-of-interest? [token]
  (let [type (:type token)]
    (not (#{:linebreak :spot} type))))

(defn interest-movement-prev-next [editor direction-fn]
  (let [cursor-id (editor/get-cursor editor)
        form-id (editor/get-focused-form-id editor)
        spatial-web (editor/get-spatial-web-for-form editor form-id)
        all-lines (filter is-token-of-interest? (apply concat (vals spatial-web)))]
    (if-let [result (direction-fn #(= (:id %) cursor-id) all-lines)]
      (editor/set-cursor editor (:id result)))))
