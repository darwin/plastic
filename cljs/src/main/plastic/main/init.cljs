(ns plastic.main.init
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.dev.devtools]
            [plastic.dev.figwheel]
            [plastic.reagent.core]
            [plastic.main.servant]
            [plastic.main.schema.core]))

; this namespace is :main entry point for cljsbuild

; this namespace exists to enforce order of requiring other namespaces
; namely init should go first, devtools and fighwheel early and main should go last

(log "PLASTIC MAIN: INIT DONE")
