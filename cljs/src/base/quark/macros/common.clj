(ns quark.macros.common
  (:refer-clojure :exclude [defonce]))

; -------------------------------------------------------------------------------------------
; mostly for figwheel support

(defmacro defonce [x & args]
  `(when-not (cljs.core/exists? ~x)
     (def ~x ~@args)))
