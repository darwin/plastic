(defproject plastic "0.1.0-SNAPSHOT"
  :description "Plastic - Experimental ClojureScript editor component for Atom"
  :url "http://github.com/darwin/plastic"

  :dependencies
  [[org.clojure/clojure "1.7.0"]
   [org.clojure/clojurescript "1.7.122"]
   [org.clojure/tools.reader "0.10.0-alpha3"]
   [org.clojure/core.async "0.1.346.0-17112a-alpha"]
   [com.cognitect/transit-cljs "0.8.225"]
   [re-frame "0.4.1"]
   [rewrite-cljs "0.3.1"]
   [binaryage/devtools "0.3.0"]
   [figwheel "0.3.8"]
   [rm-hull/inkspot "0.0.1-SNAPSHOT"]
   [spellhouse/phalanges "0.1.6"]
   [funcool/cuerdas "0.6.0"]
   [prismatic/schema "1.0.0"]
   [reagent "0.5.0" :exclusions [cljsjs/react]]
   [cljsjs/react "0.13.3-1"]]

  :plugins
  [[lein-cljsbuild "1.1.0"]
   [lein-figwheel "0.3.8"]]

  :source-paths
  ["src/macros"
   "src/env"
   "target/classes"]

  :clean-targets ^{:protect false} ["../lib/_build" "target" ".tmp"]

  :figwheel
  {:server-port    7000
   :nrepl-port     7777
   :server-logfile ".tmp/figwheel_server.log"}

  :cljsbuild
  {:builds
   {:dev
    {:source-paths ["checkouts/cljs-devtools/src"
                    "checkouts/re-frame/src"
                    "checkouts/reagent/src"
                    "checkouts/rewrite-cljs/src"
                    "checkouts/tools.reader/src/main"
                    "src/macros"
                    "src/env"
                    "src/dev"
                    "src/meld"
                    "src/common"
                    "src/main"
                    "src/worker"]
     :compiler     {:main                  plastic.main
                    :closure-defines       {"plastic.env.run_worker_on_main_thread" true
                                            "plastic.env.validate_dbs"              true
                                            "plastic.env.log_all_dispatches"        true}
                    :output-to             "../lib/_dev_build/main/plastic.js"
                    :output-dir            "../lib/_dev_build/main"
                    :optimizations         :none
                    :target                :nodejs
                    :anon-fn-naming-policy :unmapped
                    :compiler-stats        true
                    :cache-analysis        true
                    :figwheel              true
                    :source-map            true}}
    :main
    {:source-paths ["checkouts/cljs-devtools/src"
                    "checkouts/re-frame/src"
                    "checkouts/reagent/src"
                    "src/macros"
                    "src/env"
                    "src/dev"
                    "src/common"
                    "src/main"]
     :compiler     {:main                  plastic.main
                    :output-to             "../lib/_build/main/plastic.js"
                    :output-dir            "../lib/_build/main"
                    :optimizations         :none
                    :target                :nodejs
                    :anon-fn-naming-policy :unmapped
                    :compiler-stats        true
                    :cache-analysis        true
                    :figwheel              true
                    :source-map            true}}
    :worker
    {:source-paths ["checkouts/re-frame/src"
                    "checkouts/rewrite-cljs/src"
                    "checkouts/tools.reader/src/main"
                    "src/macros"
                    "src/env"
                    "src/dev"
                    "src/meld"
                    "src/common"
                    "src/worker"]
     :compiler     {:main                  plastic.worker
                    :output-to             "../lib/_build/worker/plastic.js"
                    :output-dir            "../lib/_build/worker"
                    :optimizations         :none
                    :target                :nodejs
                    :anon-fn-naming-policy :unmapped
                    :compiler-stats        true
                    :cache-analysis        true
                    :figwheel              true
                    :source-map            true}}}})
