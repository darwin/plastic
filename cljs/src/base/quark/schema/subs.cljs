(ns quark.schema.subs
  (:require [quark.frame.core :refer [register-sub]]
            [quark.schema.paths :as paths]
            [quark.util.subs :as subs])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react!]]
                   [reagent.ratom :refer [reaction]]))

(register-sub :editors (subs/path-query-factory paths/editors))

(register-sub :editor-render-state (subs/path-query-factory (fn [editor-id]
                                                              (concat paths/editors [editor-id :render-state]))))