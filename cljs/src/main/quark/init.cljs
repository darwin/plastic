(ns quark.init
  (:require [quark.dev.devtools]
            [quark.dev.figwheel]
            [quark.frame.core]
            [quark.schema.core])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

; this namespace is :main entry point for cljsbuild

; this namespace exists to enforce order of requiring other namespaces
; namely init should go first, devtools and fighwheel early and main should go last

(log "===================== QUARK INIT DONE =====================")
