(ns quark.cogs.editor.analyzer
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.whitespace :as ws]
            [clojure.zip :as z]
            [cljs.pprint :refer [pprint]]
            [cljs.tagged-literals :as tags]
            [cljs.tools.reader :as r]
            [cljs.tools.reader.reader-types :refer [string-push-back-reader]]
            [cljs.analyzer :as ana]
            [cljs.compiler :as c]
            [cljs.env :as env]
            [cljs.reader :as edn]
            [cljs.tools.reader :as reader])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [cljs.env.macros :refer [ensure with-compiler-env]]
                   [cljs.analyzer.macros :refer [no-warn]]))

(def fs (js/require "fs"))

(enable-console-print!)

; this is bleeding edge, see https://github.com/swannodette/cljs-bootstrap
; follow https://github.com/kanaka/cljs-bootstrap/blob/master/REPL.md
; => /Users/darwin/github/cljs-bootstrap

(log "ANALYZER INIT")

(def compiler-env (env/default-compiler-env))

(defn set-namespace-edn! [cenv ns-name ns-edn]
  (swap! cenv assoc-in [::ana/namespaces ns-name] (edn/read-string ns-edn)))

; load cache files
(try

  (def core-edn (.readFileSync fs "/Users/darwin/github/cljs-bootstrap/resources/cljs/core.cljs.cache.aot.edn" "utf8"))
  (def macros-edn (.readFileSync fs "/Users/darwin/github/cljs-bootstrap/.cljs_bootstrap/cljs/core$macros.cljc.cache.edn" "utf8"))

  (set-namespace-edn! compiler-env 'cljs.core core-edn)
  (set-namespace-edn! compiler-env 'cljs.core$macros macros-edn)

  (catch js/Error e
    (.log js/console (.-message e) "\n" (.-stack e) e)))

(log "ANALYZER INIT DONE")

(defn analyze-file [sources path]
  (log "analyze..." path)
  (binding [ana/*cljs-ns* 'cljs.user
            ana/*cljs-file* path
            r/*data-readers* tags/*cljs-data-readers*
            reader/*alias-map* (or reader/*alias-map* {})]
    (let [env (assoc (ana/empty-env) :context :expr
                                     :ns {:name 'cljs.user}
                                     :def-emits-var false)]
      (loop [asts []
             ns nil
             forms (seq (map #(r/read-string %) sources))]
        (if forms
          (let [form (first forms)
                ;env (assoc env :ns (get-namespace ana/*cljs-ns*))
                ast (try
                      (ana/analyze env form nil env)
                      (catch js/Error e
                        (.log js/console (.-message e) "\n" (.-stack e) e)
                        :error))
                asts (conj asts ast)]
            (if (= (:op ast) :ns)
              (recur asts (:name ast) (next forms))
              (recur asts ns (next forms))))
          [ns asts])))))