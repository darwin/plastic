(ns plastic.macros.logging)

; -------------------------------------------------------------------------------------------
; logging - these need to be macros to preserve source location for devtools

(defmacro log [& _])                                        ; do not leak ordinary logs into production

(defmacro info [& args]
  `(.info js/console ~@args))

(defmacro error [& args]
  `(.error js/console ~@args))

(defmacro warn [& args]
  `(.warn js/console ~@args))

(defmacro group [& args]
  `(.group js/console ~@args))

(defmacro group-end [& args]
  `(.groupEnd js/console ~@args))
