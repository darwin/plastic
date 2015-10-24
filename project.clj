(defproject plastic "0.1.0-SNAPSHOT"
  :description "Plastic - Experimental ClojureScript editor for Atom"
  :url "http://github.com/darwin/plastic"

  :dependencies
  [[org.clojure/clojure "1.7.0"]
   [org.clojure/clojurescript "1.7.145"]
   [org.clojure/tools.reader "0.10.0"]
   [org.clojure/core.async "0.1.346.0-17112a-alpha"]
   [com.cognitect/transit-cljs "0.8.225"]
   [devcards "0.2.0-8"]
   [re-frame "0.4.1"]
   [binaryage/devtools "0.4.0"]
   [figwheel "0.4.1"]
   [rm-hull/inkspot "0.0.1-SNAPSHOT"]
   [spellhouse/phalanges "0.1.6"]
   [funcool/cuerdas "0.6.0"]
   [prismatic/schema "1.0.1"]
   [com.rpl/specter "0.8.0"]
   [com.stuartsierra/component "0.3.0"]
   [reagent "0.5.1" :exclusions [cljsjs/react]]
   [cljsjs/react "0.13.3-1"]]

  :plugins
  [[lein-cljsbuild "1.1.0"]
   [lein-figwheel "0.4.1"]]

  :source-paths ["src"
                 "target/classes"]

  :clean-targets ^{:protect false} ["lib/_build"
                                    "target"
                                    ".tmp"
                                    "resources/devcards/_build"
                                    "resources/test/_build"]

  :figwheel
  {:server-port      7000
   :nrepl-port       7777
   :server-logfile   ".tmp/figwheel_server.log"
   :http-server-root "devcards"                                                                                       ; this will be in resources/
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
                    "checkouts/support/src"
                    "src/dev"
                    "src/meld"
                    "src/common"
                    "src/main"
                    "src/onion-atom"
                    "src/worker"]
     ; :figwheel     true <-- do not include figwheel automagically, we start it manually with our custom config
     :compiler     {:main                  plastic.boot
                    :closure-defines       {"plastic.config.dev_mode"                  true
                                            "plastic.config.run_worker_on_main_thread" true
                                            "plastic.config.need_loophole"             true
                                            ;"plastic.config.legacy_devtools"           true
                                            "plastic.config.validate_dbs"              true
                                            "plastic.config.log_all_dispatches"        true}
                    :output-to             "lib/_build/main/plastic.js"
                    :output-dir            "lib/_build/main"
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
                    "src/dev"
                    "src/devcards"
                    "src/meld"
                    "src/common"
                    "src/main"
                    "src/onion-atom"
                    "src/worker"]
     :figwheel     {:devcards true}
     :compiler     {:main                  plastic.devcards
                    :closure-defines       {"plastic.config.dont_run_loops"            true
                                            "plastic.config.run_worker_on_main_thread" true
                                            "plastic.config.validate_dbs"              true
                                            "plastic.config.log_all_dispatches"        true}
                    :output-to             "resources/devcards/_build/devcards/plastic.js"
                    :output-dir            "resources/devcards/_build/devcards"
                    :asset-path            "_build/devcards"
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
                    "src/dev"
                    "src/devcards"
                    "src/meld"
                    "src/common"
                    "src/main"
                    "src/onion-atom"
                    "src/worker"
                    "src/test"]
     :compiler     {:main                  plastic.test
                    :closure-defines       {"plastic.config.dont_run_loops"            true
                                            "plastic.config.run_worker_on_main_thread" true
                                            "plastic.config.validate_dbs"              true
                                            "plastic.config.log_all_dispatches"        true}
                    :output-to             "resources/test/_build/test/plastic.js"
                    :output-dir            "resources/test/_build/test"
                    :asset-path            "base/cljs/resources/test/_build/test"
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
                    "src/dev"
                    "src/components"
                    "src/common"
                    "src/main"
                    "src/onion-atom"]
     ; :figwheel     true <-- do not include figwheel automagically, we start it manually with our custom config
     :compiler     {:main                  plastic.main
                    :closure-defines       {"plastic.config.need_loophole"       true
                                            "plastic.config.legacy_devtools"     true
                                            "plastic.config.dont_start_figwheel" true
                                            "plastic.config.log_main_dispatches" true}
                    :output-to             "lib/_build/main/plastic.js"
                    :output-dir            "lib/_build/main"
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
                    "src/meld"
                    "src/common"
                    "src/worker"]
     :compiler     {:main                  plastic.worker
                    :closure-defines       {"plastic.config.log_worker_dispatches" true}
                    :output-to             "lib/_build/worker/plastic.js"
                    :output-dir            "lib/_build/worker"
                    :optimizations         :none
                    :target                :nodejs
                    :anon-fn-naming-policy :unmapped
                    :compiler-stats        true
                    :cache-analysis        true
                    :source-map            true
                    :source-map-timestamp  true}}}})
