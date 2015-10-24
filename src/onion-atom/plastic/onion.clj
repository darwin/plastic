(ns plastic.onion
  (:require [plastic.logging :refer [log info warn error group group-end fancy-log-with-time]]))

; -------------------------------------------------------------------------------------------------------------------

(defmacro update-inline-editor-synchronously [inline-editor-view & body]
  `(try
     (.setUpdatedSynchronously ~inline-editor-view true)
     ~@body
     nil
     (finally
       (.setUpdatedSynchronously ~inline-editor-view false)
       nil)))