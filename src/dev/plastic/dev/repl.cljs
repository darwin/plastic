(ns plastic.dev.repl
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

(defn eval [context code]
  (if (env/get context :need-loophole)
    (.runInThisContext (js/require "vm") code)                                                                        ; https://github.com/atom/loophole
    (js* "eval(~{code})")))

(defn present-repl-result [result]
  (log "%cREPL" repl-marker-style result))

(defn eval-inside-repl-plugin [context code]
  (let [rewritten-code (rewrite-repl-code-snippet code)
        result (eval context rewritten-code)]
    (present-repl-result result)
    (pr-str result)))

(defn fancy-eval [context code]
  (if *inside-repl-plugin*
    (eval-inside-repl-plugin context code)
    (eval context code)))

(defn repl-plugin [& args]
  (let [standard-impl (apply figwheel/repl-plugin args)]
    (fn [& args]
      (binding [*inside-repl-plugin* true]
        (apply standard-impl args)))))