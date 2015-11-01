(ns plastic.main.frame
  (:require [plastic.logging :refer-macros [log info warn error group group-end measure-time]]
            [re-frame.frame :refer [make-frame]]
            [plastic.frame :refer [start-loop init-frame]]
            [plastic.util.booking :refer [make-booking]]
            [plastic.onion.inface :refer [init-onion]]
            [plastic.main.frame.db :refer [make-db]]
            [plastic.main.frame.handlers :refer [register-handlers]]
            [plastic.main.frame.subs :refer [register-subs]]
            [plastic.main.servant :refer [init-servant]]))

; -------------------------------------------------------------------------------------------------------------------

(defn start [context]
  (-> context
    (assoc :worker-context (volatile! nil))
    (assoc :db (make-db context))
    (assoc :aux (make-booking))
    (init-frame)
    (init-onion)
    (init-servant)
    (register-handlers)
    (register-subs)
    (start-loop)))

(defn stop [context]
  (warn "not implemented"))