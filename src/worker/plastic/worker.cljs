(ns plastic.worker
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.worker.servant :as servant]))

; -------------------------------------------------------------------------------------------------------------------

(def ^:export dispatch-to-main servant/dispatch-to-main)
