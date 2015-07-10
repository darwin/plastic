(ns plastic.init
  (:require [plastic.dev.devtools]
            [plastic.dev.figwheel]
            [plastic.frame.core]
            [plastic.schema.core])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

; this namespace is :main entry point for cljsbuild

; this namespace exists to enforce order of requiring other namespaces
; namely init should go first, devtools and fighwheel early and main should go last

(log "===================== PLASTIC INIT DONE =====================")
