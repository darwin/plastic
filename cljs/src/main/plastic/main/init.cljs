(ns plastic.main.init
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.env]
            [plastic.dev.devtools]
            [plastic.dev.figwheel]
            [plastic.reagent.core]
            [plastic.main.servant]
            [plastic.main.schema.core]
            [plastic.onion.core]
            [plastic.cogs.boot.core]
            [plastic.main.editor.core]
            [plastic.cogs.commands.core]))

; this namespace is :main entry point for cljsbuild

; this namespace exists to enforce order of requiring other namespaces
; namely init should go first, devtools and fighwheel early and main should go last

(log "PLASTIC MAIN: INIT DONE")