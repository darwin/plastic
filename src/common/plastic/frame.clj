(ns plastic.frame
  (:require [plastic.logging :refer [log info warn error group group-end fancy-log-with-time]]
            [plastic.env :as env]))

; -------------------------------------------------------------------------------------------------------------------
; these need to be macros to preserve source location for logging into devtools

(defmacro dispatch
  ([context event] `(dispatch ~context ~event nil nil nil))
  ([context event post-handler] `(dispatch ~context ~event ~post-handler nil nil))
  ([context event post-handler pre-handler] `(dispatch ~context ~event ~post-handler ~pre-handler nil))
  ([context event post-handler pre-handler final-handler]
   `(let [context# ~context
          event# ~event]
      (if (env/get context# :log-all-dispatches)
        (fancy-log-with-time "" "DISPATCH" event#))
      (plastic.frame.dispatch* context# event# ~post-handler ~pre-handler ~final-handler))))

(defmacro worker-dispatch
  ([context event] `(worker-dispatch ~context ~event nil nil))
  ([context event post-handler] `(worker-dispatch ~context ~event ~post-handler nil))
  ([context event post-handler pre-handler]
   `(let [context# ~context
          event# ~event]
      (if (env/get context# :log-all-dispatches)
        (fancy-log-with-time "" "DISP->W" event#))
      (plastic.main.dispatch-to-worker context# event# ~post-handler ~pre-handler))))

(defmacro main-dispatch
  ([context event] `(main-dispatch ~context ~event nil nil))
  ([context event post-handler] `(main-dispatch ~context ~event ~post-handler nil))
  ([context event post-handler pre-handler]
   `(let [context# ~context
          event# ~event]
      (if (env/get context# :log-all-dispatches)
        (fancy-log-with-time "" "DISP->M" event#))
      (plastic.worker.dispatch-to-main context# event# ~post-handler ~pre-handler))))
