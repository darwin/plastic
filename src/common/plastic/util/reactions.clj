(ns plastic.util.reactions)

; -------------------------------------------------------------------------------------------------------------------

(defmacro react! [& body]
  `(let [co# (reagent.ratom/make-reaction (fn [] ~@body) :auto-run true)]
     (deref co#)
     co#))