(ns plastic.worker
  (:require [plastic.logging :refer [log info warn error group group-end fancy-log*]]))

; -------------------------------------------------------------------------------------------------------------------
; these need to be macros to preserve source location for logging into devtools

(defmacro dispatch-args [id event+args]
  `(let [event+args# ~event+args
         id# ~id]
     (if (or plastic.env.log-all-dispatches plastic.env.log-worker-dispatches)
       (fancy-log* "" "WORK" "DISPATCH" event+args# (plastic.worker.frame.current-job-desc id#)))
     (plastic.worker.frame.dispatch id# event+args#)))

(defmacro dispatch [& event+args]
  `(dispatch-args plastic.env.*current-worker-job-id* [~@event+args]))

(defmacro main-dispatch-args [id event+args]
  `(let [event+args# ~event+args
         id# ~id]
     (plastic.worker.servant.dispatch-on-main id# event+args#)))

(defmacro main-dispatch [& event+args]
  `(main-dispatch-args plastic.env.*current-worker-job-id* [~@event+args]))

(defmacro react!
  "Runs body immediately, and runs again whenever atoms deferenced in the body change. Body should side effect."
  [& body]
  `(let [co# (reagent.ratom/make-reaction (fn [] ~@body) :auto-run true)]
     (deref co#)
     co#))

(defmacro thread-zip-ops
  "Like ->> but bails on nil and does logging if enabled"
  [x & forms]
  (loop [x x, forms forms]
    (if forms
      (let [form (first forms)
            threaded `(let [x# ~x]
                        (if-not (nil? x#)
                          (let [op# ~(first form)
                                args# [~@(next form)]
                                _tmp# (if (or plastic.env.log-zip-ops plastic.env.log-threaded-zip-ops)
                                        (fancy-log* "" "WORK" "ZIPOP"
                                          (apply str (repeat plastic.env.*zip-op-nesting* "  "))
                                          (.-name op#) args# "@"
                                          (meld.zip.id (first x#))
                                          (meld.zip.desc (first x#))))
                                res# (apply op# (conj args# x#))]
                            (assert (or (nil? res#) (and (vector? res#)
                                                      (= 2 (count res#))
                                                      (meld.zip.good? (first res#))))
                              (str "zip op result must be [loc report] or nil, "
                                (.-name op#) " returned " (pr-str res#) " instead"))
                            res#)))]
        (recur threaded (next forms)))
      `(do
         (set! plastic.env.*zip-op-nesting* (inc plastic.env.*zip-op-nesting*))
         (let [r# ~x]
           (set! plastic.env.*zip-op-nesting* (dec plastic.env.*zip-op-nesting*))
           r#)))))