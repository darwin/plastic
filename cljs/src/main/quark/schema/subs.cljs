(ns quark.schema.subs
  (:require [quark.frame.core :refer [register-sub]]
            [quark.schema.paths :as paths]
            [quark.util.subs :refer [path-query-factory]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react!]]
                   [reagent.ratom :refer [reaction]]))

(defn editor-selector [path]
  (fn [editor-id]
    (concat paths/editors [editor-id] path)))

(register-sub :editors (path-query-factory paths/editors))

(register-sub :editor-render-state (path-query-factory (editor-selector [:render-state])))
(register-sub :editor-uri (path-query-factory (editor-selector [:def :uri])))
(register-sub :editor-text (path-query-factory (editor-selector [:text])))
(register-sub :editor-parse-tree (path-query-factory (editor-selector [:parse-tree])))
(register-sub :editor-cursor (path-query-factory (editor-selector [:cursor])))

(register-sub :settings (path-query-factory paths/settings))