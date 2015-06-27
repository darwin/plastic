(ns quark.schema.subs
  (:require [quark.frame.core :refer [register-sub]]
            [quark.schema.paths :as paths]
            [quark.util.subs :as subs])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react!]]
                   [reagent.ratom :refer [reaction]]))

(defn editor-selector [path]
  (fn [editor-id]
    (concat paths/editors [editor-id] path)))

(register-sub :editors (subs/path-query-factory paths/editors))

(register-sub :editor-render-state (subs/path-query-factory (editor-selector [:render-state])))
(register-sub :editor-uri (subs/path-query-factory (editor-selector [:def :uri])))
(register-sub :editor-text (subs/path-query-factory (editor-selector [:text])))
(register-sub :editor-parsed (subs/path-query-factory (editor-selector [:parsed])))