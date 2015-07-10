(ns quark.cogs.editor.editing
  (:require [quark.cogs.editor.render.dom :as dom]
            [quark.frame.core :refer [subscribe register-handler]]
            [quark.cogs.editor.model :as editor]
            [quark.schema.paths :as paths]
            [quark.cogs.editor.utils :as utils]
            [quark.onion.core :as onion])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn commit-value [editor value]
  (let [node-id (first (editor/get-editing-set editor))]
    (editor/commit-node-value editor node-id value)))

(defn apply-editing [editor op]
  (let [should-be-editing? (= op :start)]
    (editor/set-editing-set editor (if should-be-editing? (editor/get-focused-selection editor)))))

(defn start-editing [editor]
  (apply-editing editor :start))

(defn stop-editing [editor]
  (if-not (editor/editing? editor)
    editor
    (let [inline-editor (onion/get-atom-inline-editor-instance (editor/get-id editor))
          value (.getText inline-editor)]
      (dom/postpone-selection-overlay-display-until-next-update editor)
      (-> editor
        (commit-value value)
        (apply-editing :stop)))))