(ns plastic.dev.devtools
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.env]
            [devtools.core :as devtools]))

(when plastic.env.legacy-devtools
  (devtools/set-pref! :legacy-formatter true))

(log "installing cljs-devtools")

(devtools/set-pref! :install-sanity-hints true)
(devtools/install!)
