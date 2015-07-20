(ns plastic.frame
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end measure-time]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [plastic.frame.core :as frame :refer [pure trim-v log-ex]]
            [plastic.frame.router :refer [event-chan purge-chan]]
            [clojure.string :as string]
            [plastic.frame.handlers :refer [handle register-base]]
            [cljs.core.async :refer [chan put! <!]]))

(defn simple-printer [acc val]
  (conj acc (cond
              (vector? val) "[…]"
              (map? val) "{…}"
              (set? val) "#{…}"
              (string? val) "\"…\""
              :else (pr-str val))))

(defn print-simple [vec]
  (str "[" (string/join " " (reduce simple-printer [] vec)) "]"))

(defn timing [handler]
  (fn timing-handler [db v]
    (measure-time (str "H! " (print-simple v))
      (handler db v))))

(defn register-handler
  ([id handler] (register-handler id nil handler))
  ([id middleware handler] (register-base id [pure log-ex timing trim-v middleware] handler)))

(def subscribe frame/subscribe)

(defn router-loop []
  (go-loop []
    (let [event-v (<! event-chan)]
      (try
        (handle event-v)

        ;; If the handler throws:
        ;;   - allow the exception to bubble up because the app, in production,
        ;;     may have hooked window.onerror and perform special processing.
        ;;   - But an exception which bubbles up will break the enclosing go-loop.
        ;;     So we'll need to start another one.
        ;;   - purge any pending events, because they are probably related to the
        ;;     event which just fell in a screaming heap. Not sane to handle further
        ;;     events if the prior event failed.
        (catch :default e
          ;(purge-chan)
          (router-loop)
          (throw e))))
    (recur)))