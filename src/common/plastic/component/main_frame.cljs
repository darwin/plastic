(ns plastic.component.main-frame
  (:require [plastic.logging :refer-macros [log info warn error group group-end fancy-log]]
            [com.stuartsierra.component :as component]
            [plastic.frame :refer [IFrameThread]]
            [plastic.main.frame :refer [start stop]]))

; -------------------------------------------------------------------------------------------------------------------

(defrecord MainFrame [thread-id]

  IFrameThread
  (-thread-id [_] thread-id)

  component/Lifecycle
  (start [component]
    (start component))
  (stop [component]
    (stop component)))

(defn make-main-frame [thread-id]
  (MainFrame. thread-id))