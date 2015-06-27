(ns quark.cogs.editors.handlers
  (:require [quark.frame.core :refer [register-handler]]
            [quark.schema.paths :as paths])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn add-editor [editors [id editor-def]]
  (let [editors (if (map? editors) editors {})
        record {:desc "editor"
                :render-state {:some "render state"
                               :example-def editor-def}
                :editor-def editor-def}]
    (assoc editors id record)))

(defn remove-editor [editors [editor-id]]
  (if editor-id
    (dissoc editors editor-id)
    {}))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor)
(register-handler :remove-editor paths/editors-path remove-editor)