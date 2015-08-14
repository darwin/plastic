(ns plastic.worker.frame
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [plastic.worker.glue :refer [main-dispatch]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [plastic.worker.frame.core :as frame :refer [pure trim-v]]
            [plastic.worker.frame.router :refer [event-chan purge-chan]]
            [clojure.string :as string]
            [plastic.worker.frame.handlers :refer [handle register-base]]
            [cljs.core.async :refer [chan put! <!]]))

(def ^:dynamic *current-event-id* nil)
(def ^:dynamic *event-id-counters* {})

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
    (measure-time (str "WORK: PROCESS! #" *current-event-id* (print-simple v))
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

(defn update-counter [id]
  (let [old-counter (get *event-id-counters* id)
        new-counter (dec old-counter)]
    (if (zero? new-counter)
      (do
        (set! *event-id-counters* (dissoc *event-id-counters* id))
        (main-dispatch :worker-job-done id))
      (set! *event-id-counters* (assoc *event-id-counters* id new-counter)))))

(defn raise-counter [id]
  (set! *event-id-counters* (update *event-id-counters* id inc)))

(defn worker-loop []
  (go-loop []
    (let [[id event-v] (<! event-chan)]
      (binding [*current-event-id* id]
        (process-event-and-silently-swallow-exceptions event-v))
      (update-counter id)
      (recur))))

(defn dispatch [id event-v]
  (raise-counter id)
  (put! event-chan [id event-v])
  nil)