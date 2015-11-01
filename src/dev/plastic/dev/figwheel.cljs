(ns plastic.dev.figwheel
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.env :as env :include-macros true]
            [figwheel.client :as figwheel]))

; -------------------------------------------------------------------------------------------------------------------

(defonce ^:dynamic *inside-repl-plugin* false)
(defonce ^:const repl-marker-style "color:white; background-color:black; padding:0px 2px; border-radius:1px;")

; --------------------------------------------------------------------------------------------------------------------

(defn figwheel-repl-fix [code]
  (.replace code
    #"^\(function \(\)\{try\{return cljs\.core\.pr_str\.call"
    "(function (){try{return cljs.core.identity.call"))

(defn intellij-repl-fix [code]
  (.replace code
    #"^try\{cljs\.core\.pr_str\.call"
    "try{cljs.core.identity.call"))

(defn rewrite-repl-code-snippet [code]
  (-> code figwheel-repl-fix intellij-repl-fix))

(defn eval [code]
  (if plastic.config.need-loophole
    (.runInThisContext (js/require "vm") code)                                                                        ; https://github.com/atom/loophole
    (js* "eval(~{code})")))

(defn present-repl-result [result]
  (log "%cREPL" repl-marker-style result))

(defn eval-inside-repl-plugin [code]
  (let [rewritten-code (rewrite-repl-code-snippet code)
        result (eval rewritten-code)]
    (present-repl-result result)
    (pr-str result)))

(defn fancy-eval [code]
  (if *inside-repl-plugin*
    (eval-inside-repl-plugin code)
    (eval code)))

(defn repl-plugin [& args]
  (let [standard-impl (apply figwheel/repl-plugin args)]
    (fn [& args]
      (binding [*inside-repl-plugin* true]
        (apply standard-impl args)))))

(defn on-js-load [])

; --------------------------------------------------------------------------------------------------------------------

(defn start [context]
  (if-not (env/get context :dont-start-figwheel)
    (figwheel/start
      {:on-jsload     on-js-load
       :eval-fn       fancy-eval
       :websocket-url "ws://localhost:7000/figwheel-ws"
       :build-id      'dev
       :merge-plugins {:repl-plugin repl-plugin}}))
  context)

(defn stop [context]
  context)

