(ns plastic.onion.api
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]))

; these apis will be provided by host during runtime (see plastic.core)
; keeping them global is not nice, but at this point it is unrealistic to propagate context into every helper function

(defonce ^:dynamic $ nil)

(defn expose-global-apis! [apis]
  (set! $ (.-$ apis)))