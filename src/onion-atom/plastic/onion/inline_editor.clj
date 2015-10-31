(ns plastic.onion.inline-editor)

; -------------------------------------------------------------------------------------------------------------------

(defmacro update-inline-editor-synchronously [inline-editor-view & body]
  `(try
     (.setUpdatedSynchronously ~inline-editor-view true)
     ~@body
     nil
     (finally
       (.setUpdatedSynchronously ~inline-editor-view false)
       nil)))