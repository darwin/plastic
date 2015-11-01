(ns plastic.frame
  (:require [plastic.logging :refer [log info warn error group group-end fancy-log]]
            [plastic.env :as env]))

; -------------------------------------------------------------------------------------------------------------------
; these need to be macros to preserve source location for logging into devtools

(defmacro dispatch [context event]
  `(let [context# ~context
         event# ~event]
     (if (env/get context# :log-all-dispatches)
       (fancy-log "DISPATCH" event#))
     (dispatch* context# event#)))

(defmacro worker-dispatch
  ([context event] `(worker-dispatch ~context ~event nil nil))
  ([context event post-handler] `(worker-dispatch ~context ~event ~post-handler nil))
  ([context event post-handler pre-handler]
   `(let [context# ~context
          event# ~event]
      (if (env/get context# :log-all-dispatches)
        (fancy-log "DISP->W" event#))
      (plastic.main.dispatch-to-worker context# event# ~post-handler ~pre-handler))))

(defmacro main-dispatch
  ([context event] `(main-dispatch ~context ~event nil nil))
  ([context event post-handler] `(main-dispatch ~context ~event ~post-handler nil))
  ([context event post-handler pre-handler]
   `(let [context# ~context
          event# ~event]
      (if (env/get context# :log-all-dispatches)
        (fancy-log "DISP->M" event#))
      (plastic.worker.dispatch-to-main context# event# ~post-handler ~pre-handler))))