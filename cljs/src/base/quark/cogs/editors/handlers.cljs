(ns quark.cogs.editors.handlers
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [dispatch react!]]))

(defn wire-editor [editor-id]
  (let [uri-subscription (subscribe [:editor-uri editor-id])]
    (react!
      (when-let [uri @uri-subscription]
        (log "editor" editor-id "changed uri:" uri)
        (dispatch :editor-fetch-text editor-id uri)))))

(defn add-editor [editors [id editor-def]]
  (let [editors (if (map? editors) editors {})
        record {:parsed nil
                :text nil
                :render-state {:some "render state"
                               :example editor-def}
                :def editor-def}]
    (wire-editor id)
    (assoc editors id record)))

(defn remove-editor [editors [editor-id]]
  (if editor-id
    (dissoc editors editor-id)
    {}))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :add-editor paths/editors-path add-editor)
(register-handler :remove-editor paths/editors-path remove-editor)