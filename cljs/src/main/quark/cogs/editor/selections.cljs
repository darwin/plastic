(ns quark.cogs.editor.selections
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [quark.cogs.editor.utils :refer [make-zipper path->loc loc->path] :as utils]
            [clojure.zip :as z])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]))

; editor's :selections is a map of form-ids to sets of selected node-ids

(defn clear-all-selections [editors [editor-id]]
  (assoc-in editors [editor-id :selections] {}))

(defn select [editors [editor-id form-id selections]]
  (assoc-in editors [editor-id :selections] {form-id selections}))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-clear-all-selections paths/editors-path clear-all-selections)
(register-handler :editor-select paths/editors-path select)