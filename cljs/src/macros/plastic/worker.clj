(ns plastic.worker
  (:require [plastic.logging :refer [log info warn error group group-end fancy-log*]]))

; -------------------------------------------------------------------------------------------------------------------
; these need to be macros to preserve source location for logging into devtools

(defmacro dispatch-args [id event+args]
  `(let [event+args# ~event+args
         id# ~id]
     (if (or plastic.env.log-all-dispatches plastic.env.log-worker-dispatches)
       (fancy-log* "" "WORK" "DISPATCH" event+args# (plastic.worker.frame.current-job-desc id#)))
     (plastic.worker.frame.dispatch id# event+args#)))

(defmacro dispatch [& event+args]
  `(dispatch-args plastic.worker.frame.*current-job-id* [~@event+args]))

(defmacro main-dispatch-args [id event+args]
  `(let [event+args# ~event+args
         id# ~id]
     (plastic.worker.servant.dispatch-on-main id# event+args#)))

(defmacro main-dispatch [& event+args]
  `(main-dispatch-args plastic.worker.frame.*current-job-id* [~@event+args]))

(defmacro react!
  "Runs body immediately, and runs again whenever atoms deferenced in the body change. Body should side effect."
  [& body]
  `(let [co# (reagent.ratom/make-reaction (fn [] ~@body) :auto-run true)]
     (deref co#)
     co#))