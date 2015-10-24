(ns plastic.worker
  (:require [plastic.logging :refer [log info warn error group group-end fancy-log-with-time]]))

; -------------------------------------------------------------------------------------------------------------------
; these need to be macros to preserve source location for logging into devtools

(defmacro thread-zip-ops
  "Like ->> but bails on nil and does logging if enabled"
  [x & forms]
  (loop [x x
         forms forms]
    (if forms
      (let [form (first forms)
            threaded `(let [x# ~x]
                        (if-not (nil? x#)
                          (let [op# ~(first form)
                                args# [~@(next form)]
                                _tmp# (if (or plastic.config.log-zip-ops plastic.config.log-threaded-zip-ops)
                                        (fancy-log-with-time "" "ZIPOP"
                                          (apply str (repeat plastic.globals.*zip-op-nesting* "  "))
                                          (.-name op#) args# "@"
                                          (meld.zip.get-id (first x#))
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
         (set! plastic.globals.*zip-op-nesting* (inc plastic.globals.*zip-op-nesting*))
         (let [r# ~x]
           (set! plastic.globals.*zip-op-nesting* (dec plastic.globals.*zip-op-nesting*))
           r#)))))