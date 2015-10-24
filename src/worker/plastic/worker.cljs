(ns plastic.worker
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.servant :as servant]))

; -------------------------------------------------------------------------------------------------------------------

(def ^:export dispatch-to-main servant/dispatch-to-main)
