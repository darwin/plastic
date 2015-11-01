(ns plastic.dev.devtools
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.env]
            [devtools.core :as devtools]))

; -------------------------------------------------------------------------------------------------------------------

(when plastic.config.legacy-devtools
  (devtools/set-pref! :legacy-formatter true))

(log "installing cljs-devtools")

(devtools/set-pref! :install-sanity-hints true)
(devtools/install!)
