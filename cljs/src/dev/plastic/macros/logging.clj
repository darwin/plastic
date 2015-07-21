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
  `(try
     (.group js/console ~title)
     (let [res# ~@body]
       (.groupEnd js/console)
       res#)
     (catch :default e#
       (do
         (.groupEnd js/console)
         (throw e#)))))

(defmacro with-group-collapsed [title & body]
  `(try
     (.groupCollapsed js/console ~title)
     (let [res# ~@body]
       (.groupEnd js/console)
       res#)
     (catch :default e#
       (do
         (.groupEnd js/console)
         (throw e#)))))

(defmacro with-profile [title & body]
  `(try
     (.profile js/console ~title)
     (let [res# ~@body]
       (.profileEnd js/console)
       res#)
     (catch :default e#
       (do
         (.profileEnd js/console)
         (throw e#)))))

(defmacro measure-time [title & body]
  `(try
     (.time js/console ~title)
     (let [res# ~@body]
       (.timeEnd js/console ~title)
       res#)
     (catch :default e#
       (do
         (.timeEnd js/console)
         (throw e#)))))

(defmacro stopwatch [expr]
  `(let [start# (.getTime (js/Date.))
         ret# ~expr
         diff# (- (.getTime (js/Date.)) start#)]
     [ret# diff#]))