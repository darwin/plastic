(ns quark.cogs.editor.editing
  (:require [quark.cogs.editor.dom :as dom]
            [quark.cogs.editor.model :as editor])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn apply-editing [editor op]
  (let [should-be-editing? (= op :start)]
    (editor/set-editing-set editor (if should-be-editing? (editor/get-focused-selection editor)))))

(defn start-editing [editor]
  (apply-editing editor :start))

(defn stop-editing [editor]
  (dom/postpone-selection-overlay-display-until-next-update editor)
  (apply-editing editor :stop))