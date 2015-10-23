(ns plastic.node)

; we are compiling under :nodejs target
; this function has to be specified
(set! *main-cli-fn* (fn [& args] (.log js/console "main-cli-fn:" (into-array args))))