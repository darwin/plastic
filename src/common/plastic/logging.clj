(ns plastic.logging)

; -------------------------------------------------------------------------------------------------------------------
; logging - these need to be macros to preserve source location for devtools

(defmacro log [& args]
  `(do (.log js/console ~@args) nil))

(defmacro info [& args]
  `(do (.info js/console ~@args) nil))

(defmacro error [& args]
  `(do (.error js/console ~@args) nil))

(defmacro warn [& args]
  `(do (.warn js/console ~@args) nil))

(defmacro group [& args]
  `(do (.group js/console ~@args) nil))

(defmacro group-end [& args]
  `(do (.groupEnd js/console ~@args) nil))

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

(defmacro padding [s len]
  `(let [padlen# (- ~len (count ~s))]
     (apply str (repeat padlen# " "))))

(defmacro pad-str [s len]
  `(let [s# ~s]
     (str (padding s# ~len) s#)))

(defmacro fancy-log-with-time [time label & args]
  `(let [thread# (or plastic.globals.*current-thread* "JS")
         label# ~label
         thread-color# (case thread#
                         "JS" "DimGray"
                         "RENDER" "DarkPurple"
                         "MAIN" "DarkGreen"
                         "WORKER" "DarkOrange"
                         "red")
         thread-style# (str "background-color:" thread-color# ";border-radius:2px;color:white;")]
     (log "%c%s%s%c%s%c%s"
       "color:#aaa" (pad-str ~time 10)
       (padding thread# 10)
       thread-style# (if thread# (str " " thread# " ") "")
       "color:blue" (if label# (pad-str (str label# ":") 16) "")
       ~@args)))

(defmacro fancy-log [label & args]
  `(fancy-log-with-time "" ~label ~@args))

(defmacro ms [val]
  `(str (.toFixed ~val 3) "ms"))

; see https://developers.google.com/web/updates/2012/08/When-milliseconds-are-not-enough-performance-now
(defmacro measure-time [enabled? label more & body]
  `(if (or plastic.config.bench-all ~enabled?)
     (do
       (fancy-log-with-time "" ~label ~@more)
       (let [start# (.now js/performance)
             ret# (do ~@body)
             diff# (- (.now js/performance) start#)]
         (fancy-log-with-time (ms diff#) (str "<" ~label) ~@more)
         ret#))
     (let [res# (do ~@body)]
       res#)))

(defmacro stopwatch [expr]
  `(let [start# (.now js/performance)
         ret# ~expr
         diff# (- (.now js/performance) start#)]
     [ret# diff#]))

(defmacro log-render [title params & body]
  `(do
     (if plastic.config.log-rendering
       (plastic.logging/fancy-log "RENDER" ~title ~params))
     (let [res# ~@body]
       res#)))
