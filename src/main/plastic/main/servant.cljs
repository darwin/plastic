(ns plastic.main.servant
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [plastic.frame :refer [dispatch]])
  (:require [plastic.servant :as shared]
            [plastic.env :as env :include-macros true]))

; -------------------------------------------------------------------------------------------------------------------

(defn dispatch-to-worker [context & args]
  (apply shared/dispatch-to context :worker args))

(defn spawn-worker-if-needed [context]
  (if-not (env/get context :run-worker-on-main-thread)
    (let [info (get-in context [:services :services :info])
          base-path (.getLibPath info)
          worker-script (env/get context :worker-script)
          script-url (str base-path worker-script)
          new-worker (js/Worker. script-url)]
      (.addEventListener new-worker "message" #(shared/process-message context (.-data %)))
      (assoc context :worker new-worker))
    context))

(defn post-fn [context payload]
  (if-not (env/get context :run-worker-on-main-thread)
    (let [{:keys [worker]} context]
      (.postMessage worker payload))
    (let [{:keys [worker-context]} context]
      (shared/process-message @worker-context payload))))

(defn init-sockets [context]
  (-> context
    (shared/set-sockets {:worker (partial post-fn context)})))

; -------------------------------------------------------------------------------------------------------------------

(defn init-servant [context]
  (-> context
    (shared/init-servant)
    (init-sockets)
    (spawn-worker-if-needed)))