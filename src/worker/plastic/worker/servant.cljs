(ns plastic.worker.servant
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [plastic.frame :refer [dispatch]])
  (:require [plastic.servant :as shared]
            [plastic.env :as env :include-macros true]))

; -------------------------------------------------------------------------------------------------------------------

(defn dispatch-to-main [context & args]
  (apply shared/dispatch-to context :main args))

(defn post-fn [context payload]
  (if-not (env/get context :run-worker-on-main-thread)
    (let [main js/self]
      (.postMessage main payload))
    (let [{:keys [main-context]} context]
      (shared/process-message @main-context payload))))

(defn init-sockets [context]
  (-> context
    (shared/set-sockets {:main (partial post-fn context)})))

(defn register-message-handler! [context]
  (set! (.-onmessage js/self) #(shared/process-message context (.-data %)))
  context)

; -------------------------------------------------------------------------------------------------------------------

(defn init-servant [context]
  (-> context
    (shared/init-servant)
    (init-sockets)
    (register-message-handler!)))