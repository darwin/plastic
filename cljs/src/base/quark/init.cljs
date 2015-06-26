(ns quark.init
  (:require [quark.dev.devtools]
            [quark.dev.figwheel]
            [cljsjs.react]
            [quark.frame.core]
            [quark.main]))

(.log js/console "HELLO FROM QUARK INIT!")

; this namespace is :main entry point for cljsbuild

; this namespace exists to enforce order of requiring other namespaces
; namely init should go first, devtools and fighwheel early and main should go last
