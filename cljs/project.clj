(defproject plastic "0.1.0-SNAPSHOT"
  :description "Plastic - Experimental ClojureScript editor for Atom"
  :url "http://github.com/darwin/plastic"

  :dependencies
  [[org.clojure/clojure "1.7.0"]
   [org.clojure/clojurescript "1.7.122"]
   [org.clojure/tools.reader "0.10.0-alpha3"]
   [org.clojure/core.async "0.1.346.0-17112a-alpha"]
   [com.cognitect/transit-cljs "0.8.225"]
   [devcards "0.2.0-SNAPSHOT"]
   [re-frame "0.4.1"]
   [binaryage/devtools "0.3.0"]
   [figwheel "0.4.0"]
   [rm-hull/inkspot "0.0.1-SNAPSHOT"]
   [spellhouse/phalanges "0.1.6"]
   [funcool/cuerdas "0.6.0"]
   [prismatic/schema "1.0.1"]
   [reagent "0.5.1" :exclusions [cljsjs/react]]
   [cljsjs/react "0.13.3-1"]]

  :plugins
  [[lein-cljsbuild "1.1.0"]
   [lein-figwheel "0.4.0"]]

  :source-paths ["src"
                 "target/classes"]

  :clean-targets ^{:protect false} ["../lib/_build"
                                    "target"
                                    ".tmp"
                                    "resources/devcards/.build"
                                    "resources/test/.build"]

  :figwheel
  {:server-port      7000
   :nrepl-port       7777
   :server-logfile   ".tmp/figwheel_server.log"
   :http-server-root "devcards"                                                                                       ;; this will be in resources/
   :css-dirs         ["resources/devcards/css"]}

  :cljsbuild
  {:builds
   {
    ; ---------------------------------------------------------------------------------------------------------------
    :dev
    {:source-paths ["checkouts/cljs-devtools/src"
                    "checkouts/re-frame/src"
                    "checkouts/reagent/src"
                    "checkouts/tools.reader/src/main"
                    "src/env"
                    "src/dev"
                    "src/meld"
                    "src/common"
                    "src/main"
                    "src/onion-atom"
                    "src/worker"]
     ; :figwheel     true <-- do not include figwheel automagically, we start it manually with our custom config
     :compiler     {:main                  plastic.main
                    :closure-defines       {"plastic.env.run_worker_on_main_thread" true
                                            "plastic.env.need_loophole"             true
                                            "plastic.env.legacy_devtools"           true
                                            "plastic.env.validate_dbs"              true
                                            "plastic.env.log_all_dispatches"        true}
                    :output-to             "../lib/_dev_build/main/plastic.js"
                    :output-dir            "../lib/_dev_build/main"
                    :optimizations         :none
                    :target                :nodejs
                    :anon-fn-naming-policy :unmapped
                    :compiler-stats        true
                    :cache-analysis        true
                    :source-map            true
                    :source-map-timestamp  true}}

    ; ---------------------------------------------------------------------------------------------------------------
    :devcards
    {:source-paths ["checkouts/cljs-devtools/src"
                    "checkouts/re-frame/src"
                    "checkouts/reagent/src"
                    "checkouts/tools.reader/src/main"
                    "src/env"
                    "src/dev"
                    "src/devcards"
                    "src/meld"
                    "src/common"
                    "src/main"
                    "src/onion-atom"
                    "src/worker"]
     :figwheel     {:devcards true}
     :compiler     {:main                  plastic.devcards
                    :closure-defines       {"plastic.env.dont_run_loops"            true
                                            "plastic.env.run_worker_on_main_thread" true
                                            "plastic.env.validate_dbs"              true
                                            "plastic.env.log_all_dispatches"        true}
                    :output-to             "resources/devcards/.build/devcards/plastic.js"
                    :output-dir            "resources/devcards/.build/devcards"
                    :asset-path            ".build/devcards"
                    :optimizations         :none
                    :anon-fn-naming-policy :unmapped
                    :compiler-stats        true
                    :cache-analysis        true
                    :source-map            true
                    :source-map-timestamp  true}}

    ; ---------------------------------------------------------------------------------------------------------------
    :test
    {:source-paths ["checkouts/cljs-devtools/src"
                    "checkouts/re-frame/src"
                    "checkouts/reagent/src"
                    "checkouts/tools.reader/src/main"
                    "src/env"
                    "src/dev"
                    "src/devcards"
                    "src/meld"
                    "src/common"
                    "src/main"
                    "src/onion-atom"
                    "src/worker"
                    "src/test"]
     :compiler     {:main                  plastic.test
                    :closure-defines       {"plastic.env.dont_run_loops"            true
                                            "plastic.env.run_worker_on_main_thread" true
                                            "plastic.env.validate_dbs"              true
                                            "plastic.env.log_all_dispatches"        true}
                    :output-to             "resources/test/.build/test/plastic.js"
                    :output-dir            "resources/test/.build/test"
                    :asset-path            "base/cljs/resources/test/.build/test"
                    :optimizations         :none
                    :anon-fn-naming-policy :unmapped
                    :compiler-stats        true
                    :cache-analysis        true
                    :source-map            true
                    :source-map-timestamp  true}}

    ; ---------------------------------------------------------------------------------------------------------------
    :main
    {:source-paths ["checkouts/cljs-devtools/src"
                    "checkouts/re-frame/src"
                    "checkouts/reagent/src"
                    "src/env"
                    "src/dev"
                    "src/common"
                    "src/main"
                    "src/onion-atom"]
     ; :figwheel     true <-- do not include figwheel automagically, we start it manually with our custom config
     :compiler     {:main                  plastic.main
                    :closure-defines       {"plastic.env.need_loophole"       true
                                            "plastic.env.legacy_devtools"     true
                                            "plastic.env.dont_start_figwheel" true
                                            "plastic.env.log_main_dispatches" true}
                    :output-to             "../lib/_build/main/plastic.js"
                    :output-dir            "../lib/_build/main"
                    :optimizations         :none
                    :target                :nodejs
                    :anon-fn-naming-policy :unmapped
                    :compiler-stats        true
                    :cache-analysis        true
                    :source-map            true
                    :source-map-timestamp  true}}

    ; ---------------------------------------------------------------------------------------------------------------
    ; note: figwheel and devtools are not supported in web workers
    :worker
    {:source-paths ["checkouts/re-frame/src"
                    "checkouts/tools.reader/src/main"
                    "src/env"
                    "src/meld"
                    "src/common"
                    "src/worker"]
     :compiler     {:main                  plastic.worker
                    :closure-defines       {"plastic.env.log_worker_dispatches" true}
                    :output-to             "../lib/_build/worker/plastic.js"
                    :output-dir            "../lib/_build/worker"
                    :optimizations         :none
                    :target                :nodejs
                    :anon-fn-naming-policy :unmapped
                    :compiler-stats        true
                    :cache-analysis        true
                    :source-map            true
                    :source-map-timestamp  true}}}})
