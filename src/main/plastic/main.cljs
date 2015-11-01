(ns plastic.main
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.main.servant :as servant]))

; -------------------------------------------------------------------------------------------------------------------

(def ^:export dispatch-to-worker servant/dispatch-to-worker)
