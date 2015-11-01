(ns plastic.frame
  (:require [plastic.logging :refer [log info warn error group group-end fancy-log]]))

; -------------------------------------------------------------------------------------------------------------------
; these need to be macros to preserve source location for logging into devtools

(defmacro dispatch [context event]
  `(let [context# ~context
         event# ~event]
     (if (plastic.env/get context# :log-all-dispatches)
       (fancy-log "DISPATCH" event#))
     (dispatch* context# event#)))

(defmacro worker-dispatch [context event]
  `(let [context# ~context
         event# ~event]
     (if (plastic.env/get context# :log-all-dispatches)
       (fancy-log "DISP->W" event#))
     (plastic.main.dispatch-to-worker context# event#)))

(defmacro main-dispatch [context event]
  `(let [context# ~context
         event# ~event]
     (if (plastic.env/get context# :log-all-dispatches)
       (fancy-log "DISP->M" event#))
     (plastic.worker.dispatch-to-main context# event#)))
