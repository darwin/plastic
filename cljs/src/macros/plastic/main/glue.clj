(ns plastic.main.glue
  (:require [plastic.logging :refer [log info warn error group group-end fancy-log*]]))

; -------------------------------------------------------------------------------------------
; these need to be macros to preserve source location for logging into devtools

(defmacro dispatch-args [id event+args]
  `(let [event+args# ~event+args
         id# ~id]
     (fancy-log* "" "MAIN" "DISPATCH" event+args# (str "#" id#))
     (plastic.main.frame/dispatch id# event+args#)))

(defmacro dispatch [& event+args]
  `(dispatch-args 0 [~@event+args]))

(defmacro worker-dispatch-args [event+args after-effect]
  `(let [event+args# ~event+args]
     (plastic.main.servant/dispatch-on-worker event+args# ~after-effect)))

(defmacro worker-dispatch [& event+args]
  `(worker-dispatch-args [~@event+args] nil))

(defmacro worker-dispatch-with-effect [event+args effect]
  `(worker-dispatch-args ~event+args ~effect))

(defmacro react!
  "Runs body immediately, and runs again whenever atoms deferenced in the body change. Body should side effect."
  [& body]
  `(let [co# (reagent.ratom/make-reaction (fn [] ~@body) :auto-run true)]
     (deref co#)
     co#))
