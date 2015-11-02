(ns plastic.dev.figwheel
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.env :as env :include-macros true]
            [plastic.dev.repl :refer [repl-plugin echoing-eval]]
            [figwheel.client :as figwheel]))

; -------------------------------------------------------------------------------------------------------------------

(defn start [context]
  (if-not (env/get context :dont-start-figwheel)
    (figwheel/start
      {:build-id      'dev
       :websocket-url "ws://localhost:7000/figwheel-ws"
       :eval-fn       (partial echoing-eval context)
       :merge-plugins {:repl-plugin repl-plugin}}))
  context)

(defn stop [context]
  context)