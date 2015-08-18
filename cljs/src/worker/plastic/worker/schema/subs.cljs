(ns plastic.worker.schema.subs
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.worker :refer [react!]]
                   [reagent.ratom :refer [reaction]])
  (:require [plastic.worker.frame :refer [register-sub]]
            [plastic.worker.schema.paths :as paths]
            [plastic.util.subs :refer [path-query-factory]]))

(defn editor-selector [path]
  (fn [editor-id]
    (concat paths/editors [editor-id] path)))

(register-sub :editors (path-query-factory paths/editors))

(register-sub :editor-render-state (path-query-factory (editor-selector [:render-state])))
(register-sub :editor-uri (path-query-factory (editor-selector [:def :uri])))
(register-sub :editor-text (path-query-factory (editor-selector [:text])))
(register-sub :editor-parse-tree (path-query-factory (editor-selector [:parse-tree])))