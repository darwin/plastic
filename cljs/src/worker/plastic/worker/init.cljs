(ns plastic.worker.init
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.worker.schema.core]))

; this namespace is :main entry point for cljsbuild

; this namespace exists to enforce order of requiring other namespaces
; namely init should go first, devtools and fighwheel early and main should go last

(log "PLASTIC WORKER: INIT DONE")
