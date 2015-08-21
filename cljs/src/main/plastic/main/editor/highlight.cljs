(ns plastic.main.editor.highlight
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.frame :refer [register-handler]]
            [plastic.main.paths :as paths]
            [plastic.main.editor.model :as editor]
            [plastic.main.editor.toolkit.id :as id]
            [clojure.set :as set]))

; -------------------------------------------------------------------------------------------------------------------

(defn update-highlight-and-puppets [editors [selector]]
  (editor/apply-to-editors editors selector
    (fn [editor]
      (if-let [cursor-id (editor/get-cursor editor)]
        (let [id (id/id-part cursor-id)
              related-nodes (editor/find-related-ring editor id)
              puppets (set/difference related-nodes #{cursor-id})
              spot-closer-node (if (id/spot? cursor-id) #{(id/make id :closer)})]
          (-> editor
            (editor/set-highlight (set/union related-nodes spot-closer-node))
            (editor/set-puppets puppets)))))))

; -------------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-update-highlight-and-puppets paths/editors-path update-highlight-and-puppets)
