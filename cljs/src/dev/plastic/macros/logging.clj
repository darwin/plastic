(ns plastic.macros.logging)

; -------------------------------------------------------------------------------------------
; logging - these need to be macros to preserve source location for devtools

(defmacro log [& args]
  `(.log js/console ~@args))

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

(defmacro with-group [title & body]
  `(do
     (.group js/console ~title)
     (let [res# ~@body]
       (.groupEnd js/console)
       res#)))

(defmacro with-group-collpased [title & body]
  `(do
     (.groupCollapsed js/console ~title)
     (let [res# ~@body]
       (.groupEnd js/console)
       res#)))

(defmacro with-profile [title & body]
  `(do
     (.profile js/console ~title)
     (let [res# ~@body]
       (.profileEnd js/console)
       res#)))

(defmacro measure-time [title & body]
  `(do
     (.time js/console ~title)
     (let [res# ~@body]
       (.timeEnd js/console ~title)
       res#)))
