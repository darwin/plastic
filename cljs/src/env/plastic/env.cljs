(ns plastic.env)

(def ^:dynamic *current-thread* "MAIN")

; we are compiling under :nodejs target
; this function has to specified
(set! *main-cli-fn* (fn [& args] (.log js/console "main-cli-fn:" (into-array args))))

(goog-define run-worker-on-main-thread false)