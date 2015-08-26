(ns plastic.env)

; we are compiling under :nodejs target
; this function has to be specified
(set! *main-cli-fn* (fn [& args] (.log js/console "main-cli-fn:" (into-array args))))

(defonce ^:dynamic *current-thread* "MAIN")
(defonce ^:dynamic *current-main-job-id* 0)
(defonce ^:dynamic *current-main-event* nil)
(defonce ^:dynamic *current-worker-job-id* 0)
(defonce ^:dynamic *current-worker-event* nil)

(defonce ^:dynamic *zip-op-nesting* 0)

; -------------------------------------------------------------------------------------------------------------------

(goog-define run-worker-on-main-thread false)

(goog-define limit-undo-redo-queue 20)

(goog-define validate-dbs false)
(goog-define validate-main-db false)
(goog-define validate-worker-db false)

(goog-define debug-sonars false)

(goog-define bench-all false)
(goog-define bench-sonars false)
(goog-define bench-processing false)
(goog-define bench-worker-processing false)
(goog-define bench-main-processing false)
(goog-define bench-rendering false)
(goog-define bench-render-queue false)
(goog-define bench-db-validation false)
(goog-define bench-main-db-validation false)
(goog-define bench-worker-db-validation false)

(goog-define log-all-dispatches false)
(goog-define log-main-dispatches false)
(goog-define log-worker-dispatches false)
(goog-define log-rendering false)
(goog-define log-onion false)
(goog-define log-onion-inface false)
(goog-define log-inline-editor false)
(goog-define log-zip-ops true)
(goog-define log-threaded-zip-ops false)
(goog-define log-undo-redo true)
(goog-define log-parse-tree false)
