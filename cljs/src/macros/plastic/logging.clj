(ns plastic.logging)

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

(defmacro pad-str [s len]
  `(let [padlen# (- ~len (count ~s))]
     (str (apply str (repeat padlen# " ")) ~s)))

(defmacro fancy-log* [time thread label & args]
  `(let [thread# ~thread
         label# ~label
         thread-color# (case thread#
                         "MAIN" "green"
                         "WORK" "orange"
                         "red")]
     (log "%c%s%c%s%c%s"
       "color:#aaa" (pad-str ~time 10)
       (str "color:" thread-color#) (if thread# (pad-str (str "[" thread# "]") 10) "")
       "color:blue" (if label# (pad-str (str label# ":") 12) "")
       ~@args)))

(defmacro fancy-log [label & args]
  `(fancy-log* "" plastic.env/*current-thread* ~label ~@args))

(defmacro ms [val]
  `(str (.toFixed ~val 3) "ms"))

; see https://developers.google.com/web/updates/2012/08/When-milliseconds-are-not-enough-performance-now
(defmacro measure-time [enabled? label more & body]
  `(if (or plastic.env/bench-all ~enabled?)
     (let [start# (.now js/performance)
           ret# ~@body
           diff# (- (.now js/performance) start#)]
       (fancy-log* (ms diff#) plastic.env/*current-thread* ~label ~@more)
       ret#)
     (let [res# ~@body]
       res#)))

(defmacro stopwatch [expr]
  `(let [start# (.now js/performance)
         ret# ~expr
         diff# (- (.now js/performance) start#)]
     [ret# diff#]))