(ns plastic.config)

; -------------------------------------------------------------------------------------------------------------------
; code should not access these directly (except for system bootstrapping)
; environment should be passed into components where it should be accessed via env/get, env/or or similar
;
; you can override these defaults from project.clj via :closure-defines
;   see http://www.martinklepsch.org/posts/parameterizing-clojurescript-builds.html
;
; -------------------------------------------------------------------------------------------------------------------

(goog-define worker-script "/worker.js")

(goog-define dev-mode false)

(goog-define dont-start-figwheel false)
(goog-define legacy-devtools false)
(goog-define need-loophole false)
(goog-define dont-run-loops false)
(goog-define run-worker-on-main-thread false)

(goog-define limit-undo-redo-queue 20)

(goog-define validate-dbs false)
(goog-define validate-main-db false)
(goog-define validate-worker-db false)

(goog-define debug-sonars false)

(goog-define bench-all false)
(goog-define bench-sonars false)
(goog-define bench-processing false)
(goog-define bench-rendering false)
(goog-define bench-render-queue false)
(goog-define bench-db-validation false)
(goog-define bench-main-db-validation false)
(goog-define bench-worker-db-validation false)
(goog-define bench-transit false)
(goog-define bench-worker-transit false)
(goog-define bench-main-transit false)

(goog-define log-all-dispatches false)
(goog-define log-app-db false)
(goog-define log-main-dispatches false)
(goog-define log-worker-dispatches false)
(goog-define log-rendering false)
(goog-define log-onion false)
(goog-define log-inline-editor false)
(goog-define log-zip-ops false)
(goog-define log-threaded-zip-ops false)
(goog-define log-undo-redo false)
(goog-define log-parse-tree false)