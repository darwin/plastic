(ns quark.cogs.editors.subs
  (:require [quark.frame.core :refer [register-sub subscribe]]
            [quark.schema.paths :as paths]
            [quark.util.subs :refer [get-in-query-factory]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react!]]))

(register-sub :editors (get-in-query-factory paths/editors))

(def editors-substription (subscribe [:editors]))

(react!
  (if-let [editors @editors-substription]
    (log "editors changed:" editors)))