(ns plastic.main.editor.highlight
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.frame :refer [register-handler]]
            [plastic.main.paths :as paths]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.ops.editing :as editing]
            [plastic.main.editor.toolkit.id :as id]))

; -------------------------------------------------------------------------------------------------------------------

(defn update-highlight [editors [selector]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (if-let [cursor-id (editor/get-cursor editor)]
        (let [id (id/id-part cursor-id)
              highlight (if (id/spot? cursor-id)
                          #{(id/make id :closer)}
                          (editor/find-related-ring editor id))]
          (editor/set-highlight editor highlight))))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-update-highlight paths/editors-path update-highlight)
