(ns plastic.main
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.servant :as servant]))

; -------------------------------------------------------------------------------------------------------------------

(def ^:export dispatch-to-worker servant/dispatch-to-worker)
