(ns quark.cogs.editor.editing
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn apply-editing [editor op]
  (let [editing? (= op :start)
        selections (:selections editor)
        focused-form-id (:focused-form-id selections)
        selection (get selections focused-form-id)]
    (assoc-in editor [:editing] (if editing? selection #{}))))

(defn start-editing [editor]
  (apply-editing editor :start))

(defn stop-editing [editor]
  (apply-editing editor :stop))