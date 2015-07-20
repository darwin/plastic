(ns plastic.frame
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end measure-time]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [plastic.frame.core :as frame :refer [pure trim-v]]
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

(defn log-ex [handler]
  (fn log-ex-handler [db v]
    (try
      (handler db v)
      (catch js/Error e                                     ;  You don't need it any more IF YOU ARE USING CHROME 44. Chrome now seems to now produce good stack traces.
        (do
          (.error js/console (.-stack e))
          (throw e)))
      (catch :default e
        (do
          (.error js/console e)
          (throw e))))))

(defn register-handler
  ([id handler] (register-handler id nil handler))
  ([id middleware handler] (register-base id [pure log-ex timing trim-v middleware] handler)))

(def subscribe frame/subscribe)

(defn process-event-and-silently-swallow-exceptions [event-v]
  (try
    (handle event-v)
    (catch :default _)))

(defn router-loop []
  (go-loop []
    (let [event-v (<! event-chan)]
      (process-event-and-silently-swallow-exceptions event-v)
      (recur))))