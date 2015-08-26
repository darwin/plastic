(ns plastic.dev.devtools
  (:require [devtools.core :as devtools]))

(devtools/set-pref! :legacy-formatter true)

(devtools/install!)
