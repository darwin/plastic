(ns quark.schema.subs
  (:require [quark.frame.core :refer [register-sub]]
            [quark.schema.paths :as paths]
            [quark.util.subs :refer [get-in-query-factory]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react!]]
                   [reagent.ratom :refer [reaction]]))

(register-sub :editors (get-in-query-factory paths/editors))

(register-sub :editor-render-state (fn [db [_ editor-id]]
                                     (reaction (get-in @db (concat paths/editors [editor-id :render-state])))))