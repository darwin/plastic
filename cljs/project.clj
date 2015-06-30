(defproject quark "0.1.0-SNAPSHOT"
  :description "Quark - Experimental ClojureScript editor component for Atom"
  :url "http://github.com/darwin/quark"

  :dependencies [[org.clojure/clojure "1.7.0-beta3"]
                 [org.clojure/clojurescript "0.0-3422"]
                 [org.clojure/tools.reader "0.10.0-SNAPSHOT"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [rewrite-cljs "0.2.0"]
                 [binaryage/devtools "0.1.2"]               ; Electron 0.28.2 has old Blink, we have to stick with this old version of devtools for now
                 [figwheel "0.3.3"]
                 [rm-hull/inkspot "0.0.1-SNAPSHOT"]
                 [reagent "0.5.0" :exclusions [cljsjs/react]]
                 [cljsjs/react "0.12.2-5"]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-figwheel "0.3.3"]]

  :source-paths ["src/base"
                 "src/dev"
                 "target/classes"
                 "checkouts/tools.reader/src/main/clojure"]

  :clean-targets ^{:protect false} ["../lib/_build" "target" ".tmp"]

  :figwheel {:server-port    7000
             :nrepl-port     7777
             :server-logfile ".tmp/figwheel_server.log"}

  :cljsbuild {:builds [{:id           "dev"
                        :source-paths ["src/base"
                                       "checkouts/clojurescript/src/main/cljs"
                                       ;"checkouts/clojurescript/src/main/clojure"
                                       "checkouts/tools.reader/src/main/cljs"
                                       "checkouts/reagent/src"
                                       "checkouts/figwheel/src"
                                       "checkouts/rewrite-cljs/src"]
                        :compiler     {:main           quark.init
                                       :output-to      "../lib/_build/quark.js"
                                       :output-dir     "../lib/_build"
                                       :optimizations  :none
                                       :target         :nodejs
                                       :compiler-stats true
                                       :cache-analysis true
                                       :figwheel       true
                                       :source-map     true}}]})
