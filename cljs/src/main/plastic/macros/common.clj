(ns plastic.macros.common
  (:refer-clojure :exclude [defonce]))

; -------------------------------------------------------------------------------------------
; mostly for figwheel support

(defmacro defonce [x & args]
  `(when-not (cljs.core/exists? ~x)
     (def ~x ~@args)))

(defmacro *->
  "like -> but nil results are no-ops to input"
  [expr & forms]
  (let [g (gensym)
             pstep (fn [step] `(if (nil? ~g) ~g (-> ~g ~step)))]
    `(let [~g ~expr
           ~@(interleave (repeat g) (map pstep forms))]
       ~g)))