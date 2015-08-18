(ns plastic.main.frame
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [reagent.core :as reagent]
            [plastic.main.db :refer [db]]
            [re-frame.middleware :as middleware]
            [re-frame.frame :as frame]
            [re-frame.scaffold :as scaffold]
            [re-frame.utils :as utils]))

(defonce ^:dynamic *current-job-id* nil)
(defonce ^:dynamic *jobs* {})
(defonce ^:private event-chan (chan))
(defonce main-frame (atom (frame/make-frame)))

(defn timing [handler]
  (fn timing-handler [db v]
    (measure-time (or plastic.env.bench-processing plastic.env.bench-main-processing) "PROCESS" [v (str "#" *current-job-id*)]
      (handler db v))))

(def path (middleware/path main-frame))

(def common-middleware [timing (middleware/trim-v main-frame)])

(defn register-handler
  ([event-id handler-fn] (register-handler event-id nil handler-fn))
  ([event-id middleware handler-fn] (scaffold/register-base main-frame event-id [common-middleware middleware] handler-fn)))

(defn register-sub [subscription-id handler-fn]
  (swap! main-frame #(frame/register-subscription-handler % subscription-id handler-fn)))

(defn subscribe [subscription-spec]
  (scaffold/legacy-subscribe main-frame db subscription-spec))

(defn ^:export dispatch [job-id event]
  (put! event-chan [job-id event])
  nil)

(defn register-job [job-id continuation]
  (set! *jobs* (assoc *jobs* job-id {:events       []
                                     :continuation continuation})))

(defn unregister-job [job-id]
  (set! *jobs* (dissoc *jobs* job-id)))

(defn buffer-job-event [job-id event]
  (set! *jobs* (update-in *jobs* [job-id :events] (fn [events] (conj events event)))))

(defn handle-event-and-report-exceptions [frame-atom db event]
  (try
    (frame/process-event-on-atom @frame-atom db event)
    (catch :default e
      (error e (.-stack e)))))

(defn job-done [db [job-id]]
  (let [job (get *jobs* job-id)
        _ (assert job)
        coallesced-db (reagent/atom db)]
    (doseq [event (:events job)]                            ; replay all buffered job events...
      (handle-event-and-report-exceptions main-frame coallesced-db event))
    (unregister-job job-id)
    (or ((:continuation job) @coallesced-db) db)))          ; contination can decide not to publish results (and maybe apply them later)

(register-handler :worker-job-done job-done)

(defn main-loop []
  (binding [plastic.env/*current-thread* "MAIN"]
    (go-loop []
      (let [[job-id event] (<! event-chan)]
        (if (zero? job-id)
          (handle-event-and-report-exceptions main-frame db event)
          (buffer-job-event job-id event)))
      (recur))))
