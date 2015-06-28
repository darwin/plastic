(ns quark.cogs.editor.analyzer
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.whitespace :as ws]
            [clojure.zip :as z]
            [cljs.pprint :refer [pprint]]
            [cljs.tagged-literals :as tags]
            [cljs.tools.reader :as reader]
            [cljs.tools.reader.reader-types :refer [string-push-back-reader indexing-push-back-reader PushbackReader]]
            [cljs.analyzer :as ana]
            [cljs.compiler :as c]
            [cljs.env :as env]
            [cljs.reader :as edn]
            [cljs.tools.reader :as reader]
            [clojure.string :as string])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [cljs.env.macros :refer [ensure with-compiler-env]]
                   [cljs.analyzer.macros :refer [no-warn]]))

(def fs (js/require "fs"))

(enable-console-print!)

(defn ^:private custom-warning-handler [warning-type env extra]
  (when (warning-type ana/*cljs-warnings*)
    (when-let [s (ana/error-message warning-type extra)]
      (throw s))))

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

(declare analyze-file)

(defn locate-src
  "Given a namespace return the corresponding ClojureScript (.cljs or .cljc)
  resource on the classpath or file from the root of the build."
  [ns]
  (info "locate src" ns)
  "")


(defn analyze-deps
  ([lib deps env] (analyze-deps lib deps env nil))
  ([lib deps env opts]
   (let [compiler @env/*compiler*]
     (binding [ana/*cljs-dep-set* (vary-meta (conj ana/*cljs-dep-set* lib) update-in [:dep-path] conj lib)]
       (assert (every? #(not (contains? ana/*cljs-dep-set* %)) deps)
         (str "Circular dependency detected " (-> ana/*cljs-dep-set* meta :dep-path)))
       (doseq [dep deps]
         (when-not (or (not-empty (get-in compiler [::namespaces dep :defs]))
                     (contains? (:js-dependency-index compiler) (name dep))
                     ;#?(:clj (deps/find-classpath-lib dep))
                     )
           (if-let [src (locate-src dep)]
             (analyze-file src opts)
             (throw
               (ana/error env
                 (ana/error-message :undeclared-ns {:ns-sym dep :js-provide (name dep)}))))))))))

(defmethod ana/parse 'ns
  [_ env [_ name & args :as form] _ opts]
  (when-not (symbol? name)
    (throw (ana/error env "Namespaces must be named by a symbol.")))
  (let [name (cond-> name (:macros-ns opts) ana/macro-ns-name)]
    (let [segments (string/split (clojure.core/name name) #"\.")]
      (when (= 1 (count segments))
        (ana/warning :single-segment-namespace env {:name name}))
      (when (some ana/js-reserved segments)
        (ana/warning :munged-namespace env {:name name}))
      (ana/find-def-clash env name segments))
    (let [docstring (if (string? (first args)) (first args))
          mdocstr (-> name meta :doc)
          args (if docstring (next args) args)
          metadata (if (map? (first args)) (first args))
          form-meta (meta form)
          args (ana/desugar-ns-specs (if metadata (next args) args))
          name (vary-meta name merge metadata)
          excludes (ana/parse-ns-excludes env args)
          deps (atom #{})
          aliases (atom {:fns {} :macros {}})
          spec-parsers {:require        (partial ana/parse-require-spec env false deps aliases)
                        :require-macros (partial ana/parse-require-spec env true deps aliases)
                        :use            (comp (partial ana/parse-require-spec env false deps aliases)
                                          (partial ana/use->require env))
                        :use-macros     (comp (partial ana/parse-require-spec env true deps aliases)
                                          (partial ana/use->require env))
                        :import         (partial ana/parse-import-spec env deps)}
          valid-forms (atom #{:use :use-macros :require :require-macros :import})
          reload (atom {:use nil :require nil :use-macros nil :require-macros nil})
          reloads (atom {})
          {uses :use requires :require use-macros :use-macros require-macros :require-macros imports :import :as params}
          (reduce
            (fn [m [k & libs]]
              (when-not (#{:use :use-macros :require :require-macros :import} k)
                (throw (ana/error env "Only :refer-clojure, :require, :require-macros, :use and :use-macros libspecs supported")))
              (when-not (@valid-forms k)
                (throw (ana/error env (str "Only one " k " form is allowed per namespace definition"))))
              (swap! valid-forms disj k)
              ;; check for spec type reloads
              (when-not (= :import k)
                (when (some #{:reload} libs)
                  (swap! reload assoc k :reload))
                (when (some #{:reload-all} libs)
                  (swap! reload assoc k :reload-all)))
              (apply merge-with merge m
                (map (spec-parsers k)
                  (remove #{:reload :reload-all} libs))))
            {} (remove (fn [[r]] (= r :refer-clojure)) args))]
      (when (and ana/*analyze-deps* (seq @deps))
        (analyze-deps name @deps env (dissoc opts :macros-ns)))
      (when (and ana/*analyze-deps* (seq uses))
        (ana/check-uses uses env))
      (set! ana/*cljs-ns* name))))

(deftype Sentinel [])

(defn ext
  "Given a file, url or string return the file extension."
  [x]
    (last (string/split x #"\.")))

(defn forms-seq*
  "Seq of Clojure/ClojureScript forms from rdr, a java.io.Reader. Optionally
  accepts a filename argument which will be used in any emitted errors."
  ([source] (forms-seq* source nil))
  ([source filename]
   (let [eof-sentinel (Sentinel.)
         opts (merge
                {:eof eof-sentinel}
                (if (and filename (= (ext filename) "cljc"))
                  {:read-cond :allow :features #{:cljs}}))
         pbr (indexing-push-back-reader source 1 filename)
         data-readers tags/*cljs-data-readers*
         forms-seq_
         (fn forms-seq_ []
           (lazy-seq
             (let [form (binding [*ns* (create-ns ana/*cljs-ns*)
                                  reader/*data-readers* data-readers
                                  reader/*alias-map*
                                  (apply merge
                                    ((juxt :requires :require-macros)
                                      (ana/get-namespace ana/*cljs-ns*)))
                                  reader/resolve-symbol ana/resolve-symbol]
                          (reader/read opts pbr))]
               (if-not (identical? form eof-sentinel)
                 (cons form (forms-seq_))))))]
     (forms-seq_))))

(defn analyze-file
  ([source] (analyze-file source nil))
  ([source opts]
   (binding [ana/*file-defs* (atom #{})]
     (ensure
       (let [;ns-info (ana/parse-ns res)
             path (or (:atom-path opts) "unknown/file/path.clj?")
             opts (dissoc opts :atom-path)]
         ;(when-not (get-in @env/*compiler* [::namespaces (:ns ns-info) :defs])
         (binding [ana/*cljs-ns* 'cljs.user
                   ana/*cljs-file* path
                   reader/*alias-map* (or reader/*alias-map* {})]
           (let [env (assoc (ana/empty-env) :build-options opts)
                 forms-todo (seq (forms-seq* source))]
             (loop [ns nil
                    forms forms-todo]
               (if forms
                 (let [form (first forms)
                       env (assoc env :ns (ana/get-namespace ana/*cljs-ns*))
                       _ (log "analyze" form env opts)
                       ast (ana/analyze env form nil opts)]
                   (if (= (:op ast) :ns)
                     (recur (:name ast) (next forms))
                     (recur ns (next forms))))
                 ns)))))))))