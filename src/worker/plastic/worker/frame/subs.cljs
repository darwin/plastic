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

(defn define-subscription [context name selector]
  (register-sub context name (path-query-factory context selector)))

(defn register-subs [context]
  (-> context
    (define-subscription :editors [:editors])
    (define-subscription :editor (editor-selector []))
    (define-subscription :editor-render-state (editor-selector [:render-state]))
    (define-subscription :editor-forms (editor-selector [:forms]))
    (define-subscription :editor-uri (editor-selector [:uri]))
    (define-subscription :editor-text (editor-selector [:text]))
    (define-subscription :editor-parse-tree (editor-selector [:parse-tree]))
    (define-subscription :editor-xform-report (editor-selector [:xform-report]))))