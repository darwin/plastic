(ns plastic.macros.glue
  (:require [plastic.macros.logging :refer [log info warn error group group-end]]))

; -------------------------------------------------------------------------------------------
; these need to be macros to preserve source location for logging into devtools

(defmacro dispatch-args [event+args]
  `(let [event+args# ~event+args]
     (log "D!" event+args#)
     (plastic.frame.core/dispatch event+args#)))

(defmacro dispatch [& event+args]
  `(dispatch-args [~@event+args]))

(defmacro react!
  "Runs body immediately, and runs again whenever atoms deferenced in the body change. Body should side effect."
  [& body]
  `(let [co# (reagent.ratom/make-reaction (fn [] ~@body) :auto-run true)]
     (deref co#)
     co#))
