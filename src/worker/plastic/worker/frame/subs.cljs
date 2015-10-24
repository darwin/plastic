(ns plastic.worker.frame.subs
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [reagent.ratom :refer [reaction]])
  (:require [plastic.frame :refer [register-sub]]
            [plastic.util.subs :refer [path-query-factory]]))

; -------------------------------------------------------------------------------------------------------------------

(defn editor-selector [path]
  (fn [editor-id]
    (concat [:editors editor-id] path)))

; -------------------------------------------------------------------------------------------------------------------

(defn register-subs [context]
  (-> context
    (register-sub :editors (path-query-factory [:editors]))
    (register-sub :editor (path-query-factory (editor-selector [])))
    (register-sub :editor-render-state (path-query-factory (editor-selector [:render-state])))
    (register-sub :editor-forms (path-query-factory (editor-selector [:forms])))
    (register-sub :editor-uri (path-query-factory (editor-selector [:uri])))
    (register-sub :editor-text (path-query-factory (editor-selector [:text])))
    (register-sub :editor-parse-tree (path-query-factory (editor-selector [:parse-tree])))
    (register-sub :editor-xform-report (path-query-factory (editor-selector [:xform-report])))))