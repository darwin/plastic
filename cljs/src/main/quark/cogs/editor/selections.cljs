(ns quark.cogs.editor.selections
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [quark.cogs.editor.utils :refer [make-zipper path->loc loc->path] :as utils]
            [quark.cogs.editor.selection.model :as model]
            [clojure.zip :as z])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]))

; editor's :selections is a map of form-ids to sets of selected node-ids
; also has key :focused-form-id pointing to currently focused form

(defn clear-all-selections [editors [editor-id]]
  (let [last-focused-form-id (get-in editors [editor-id :selections :focused-form-id])]
    (assoc-in editors [editor-id :selections] {:focused-form-id last-focused-form-id})))

(defn select [editors [editor-id form-id selections]]
  (assoc-in editors [editor-id :selections] {:focused-form-id form-id
                                             form-id        selections}))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-clear-all-selections paths/editors-path clear-all-selections)
(register-handler :editor-select paths/editors-path select)
