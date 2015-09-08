(ns plastic.dev.devtools
  (:require [plastic.env]
            [devtools.core :as devtools]))

(when plastic.env.legacy-devtools
  (devtools/set-pref! :legacy-formatter true))

(devtools/install!)
