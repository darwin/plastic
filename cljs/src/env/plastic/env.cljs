(ns plastic.env)

; we are compiling under :nodejs target
; this function has to be specified
(set! *main-cli-fn* (fn [& args] (.log js/console "main-cli-fn:" (into-array args))))

(def ^:dynamic *current-thread* "MAIN")

; ----------------------------------------------------------------------------------------

(goog-define run-worker-on-main-thread false)

(goog-define debug-sonars false)

(goog-define bench-all false)
(goog-define bench-sonars false)
(goog-define bench-processing false)
(goog-define bench-worker-processing false)
(goog-define bench-main-processing false)
(goog-define bench-rendering false)
(goog-define bench-render-queue false)

(goog-define log-all-dispatches false)
(goog-define log-main-dispatches false)
(goog-define log-worker-dispatches false)
(goog-define log-rendering false)
(goog-define log-onion false)
(goog-define log-onion-inface false)
(goog-define log-inline-editor false)
