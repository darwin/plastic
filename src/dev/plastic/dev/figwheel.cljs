(ns plastic.dev.figwheel
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.env]
            [clojure.string :as string]
            [figwheel.client :as figwheel]
            [figwheel.client.socket :as socket]
            [figwheel.client.utils :as utils]))

(defn eval [code]
  (if plastic.env.need-loophole
    (.runInThisContext (js/require "vm") code)                                                                        ; https://github.com/atom/loophole
    (js* "eval(~{code})")))

(defn figwheel-repl-fix [code]
  (.replace code
    #"^\(function \(\)\{try\{return cljs\.core\.pr_str\.call"
    "(function (){try{return cljs.core.identity.call"))

(defn intellij-repl-fix [code]
  (.replace code #"^try\{cljs\.core\.pr_str\.call" "try{cljs.core.identity.call"))

; I wasn't able to remove pr-str wrapper on figwheel side, this is a dirty hack
(defn patch-code [code]
  (-> code figwheel-repl-fix intellij-repl-fix))

(defn eval-javascript** [code result-handler]
  (try
    (binding [*print-fn* (fn [& args] (-> args figwheel/console-print figwheel/figwheel-repl-print))
              *print-newline* false]
      (let [res (eval code)]
        (if res
          (.log js/console "F>" res))
        (result-handler {:status :success
                         :value  (pr-str res)})))
    (catch js/Error e
      (result-handler {:status     :exception
                       :value      (pr-str e)
                       :stacktrace (string/join "\n" (figwheel/truncate-stack-trace (.-stack e)))
                       :base-path  (utils/base-url-path)}))
    (catch :default e
      (result-handler {:status     :exception
                       :value      (pr-str e)
                       :stacktrace "No stacktrace available."}))))

(defn repl-plugin [_]
  (fn [[{:keys [msg-name] :as msg} & _]]
    (when (= :repl-eval msg-name)
      (figwheel/ensure-cljs-user)
      (eval-javascript** (patch-code (:code msg))
        (fn [res]
          (socket/send! {:figwheel-event "callback"
                         :callback-name  (:callback-name msg)
                         :content        res}))))))

(defn on-js-load []
  ;(.remount-editors js/plastic.onion.remounter)
  ;(dispatch :editor-update-layout)
  )

(when-not plastic.env.dont-start-figwheel
  (figwheel/start
    {:on-jsload     on-js-load
     :eval-fn       eval
     :websocket-url "ws://localhost:7000/figwheel-ws"
     :build-id      'dev
     ; :merge-plugins {:repl-plugin repl-plugin}
     }))
