(ns plastic.main.frame
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [plastic.main.frame.core :as frame :refer [pure trim-v]]
            [plastic.main.frame.router :refer [event-chan purge-chan]]
            [plastic.main.frame.handlers :refer [handle register-base *handling*]]
            [plastic.main.db :refer [db]]
            [reagent.core :as reagent]
            [cljs.core.async :refer [chan put! <!]]))

(defonce ^:dynamic *current-job-id* nil)
(defonce ^:dynamic *jobs* {})

(defn register-job [job-id continuation]
  (set! *jobs* (assoc *jobs* job-id {:events       []
                                     :continuation continuation})))

(defn unregister-job [job-id]
  (set! *jobs* (dissoc *jobs* job-id)))

(defn buffer-job-event [job-id event]
  (set! *jobs* (update-in *jobs* [job-id :events] (fn [events] (conj events event)))))

(defn timing [handler]
  (fn timing-handler [db v]
    (measure-time (or plastic.env.bench-processing plastic.env.bench-main-processing) "PROCESS" [v (str "#" *current-job-id*)]
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

(def subscribe (partial frame/subscribe db))

(defn handle-event-and-silently-swallow-exceptions [db event]
  (try
    (handle db event)                                       ; let re-frame handle the event
    (catch :default _)))

(defn main-loop [db]
  (go-loop []
    (let [[job-id event] (<! event-chan)]
      (binding [plastic.env/*current-thread* "MAIN"]
        (if (zero? job-id)
          (handle-event-and-silently-swallow-exceptions db event)
          (buffer-job-event job-id event)))
      (recur))))

(defn ^:export dispatch [job-id event]
  (put! event-chan [job-id event])
  nil)

(defn job-done [db [job-id]]
  (let [job (get *jobs* job-id)
        _ (assert job)
        coallesced-db (reagent/atom db)]
    (binding [*handling* false]                             ; suppress warning in handle call, here we operate on a separate db
      (doseq [event (:events job)]                          ; replay all buffered job events...
        (handle-event-and-silently-swallow-exceptions coallesced-db event)))
    (unregister-job job-id)
    (or ((:continuation job) @coallesced-db) db)))          ; contination can decide not to publish results (and maybe apply them later)

(register-handler :worker-job-done job-done)