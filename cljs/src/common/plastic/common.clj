(ns plastic.common
  (:refer-clojure :exclude [defonce]))

; -------------------------------------------------------------------------------------------------------------------
; mostly for figwheel support

(defmacro defonce [x & args]
  `(when-not (cljs.core/exists? ~x)
     (def ~x ~@args)))

; this allows to enter reducing function as the last param for better code formatting
(defmacro process
  ([coll f] `(cljs.core/reduce ~f ~coll))
  ([coll init f] `(cljs.core/reduce ~f ~init ~coll)))
