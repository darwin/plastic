(ns plastic.schema.subs
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react!]]
                   [reagent.ratom :refer [reaction]])
  (:require [plastic.frame.core :refer [register-sub]]
            [plastic.schema.paths :as paths]
            [plastic.util.subs :refer [path-query-factory]]))

(defn editor-selector [path]
  (fn [editor-id]
    (concat paths/editors [editor-id] path)))

(defn settings-selector []
  (fn [setting]
    (concat paths/settings [setting])))


(register-sub :editors (path-query-factory paths/editors))

(register-sub :editor-render-state (path-query-factory (editor-selector [:render-state])))
(register-sub :editor-uri (path-query-factory (editor-selector [:def :uri])))
(register-sub :editor-text (path-query-factory (editor-selector [:text])))
(register-sub :editor-parse-tree (path-query-factory (editor-selector [:parse-tree])))
(register-sub :editor-cursors (path-query-factory (editor-selector [:cursors])))
(register-sub :editor-selection (path-query-factory (editor-selector [:selection])))
(register-sub :editor-editing (path-query-factory (editor-selector [:editing])))

(register-sub :settings (path-query-factory (settings-selector)))