(ns plastic.env)

; we are compiling under :nodejs target
; this function has to specified
(set! *main-cli-fn* (fn [& args] (.log js/console "main-cli-fn called:" args)))

(goog-define run-worker-on-main-thread false)