(ns plastic.cogs.editor.editing
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.cogs.editor.render.dom :as dom]
            [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.cogs.editor.model :as editor]
            [plastic.onion.core :as onion]))

(defn commit-value [editor value]
  (let [node-id (first (editor/get-editing-set editor))]
    (editor/commit-node-value editor node-id value)))

(defn apply-editing [editor op]
  (let [should-be-editing? (= op :start)
        can-edit? (editor/can-edit-focused-selection? editor)]
    (editor/set-editing-set editor (if (and can-edit? should-be-editing?) (editor/get-focused-selection editor)))))

(defn start-editing [editor]
  (apply-editing editor :start))

(defn stop-editing [editor]
  (or
    (if (editor/editing? editor)
      (let [editor-id (editor/get-id editor)
            modified-editor (if (or true (onion/is-inline-editor-modified? editor-id))
                              (commit-value editor (onion/get-postprocessed-text-after-editing editor-id))
                              (do (log "not modified")
                                  editor))]
        (dom/postpone-selection-overlay-display-until-next-update editor)
        (apply-editing modified-editor :stop)))
    editor))