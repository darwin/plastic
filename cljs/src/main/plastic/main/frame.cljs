(ns plastic.main.frame
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [cljs.core.async :refer [chan put! <!]]
            [reagent.core :as reagent]
            [plastic.main.db :refer [db]]
            [plastic.main.frame.jobs :as jobs]
            [re-frame.middleware :as middleware]
            [re-frame.frame :as frame]
            [re-frame.scaffold :as scaffold]
            [re-frame.utils :as utils]))

(defonce ^:dynamic *current-job-id* nil)
(defonce ^:private event-chan (chan))
(defonce frame (atom (frame/make-frame)))

(defn current-job-desc
  ([] (current-job-desc *current-job-id*))
  ([id] (if-not (zero? id) (str "(job " id ")") "")))

(def bench? (or plastic.env.bench-processing plastic.env.bench-main-processing))

(defn timing [handler]
  (fn timing-handler [db v]
    (measure-time bench? "PROCESS" [v (current-job-desc)]
      (handler db v))))

(def register-job jobs/register-job)

(def path (middleware/path frame))

(def common-middleware [timing (middleware/trim-v frame)])

(defn register-handler
  ([event-id handler-fn]
   (register-handler event-id nil handler-fn))
  ([event-id middleware handler-fn]
   (scaffold/register-base frame event-id [common-middleware middleware] handler-fn)))

(defn register-sub [subscription-id handler-fn]
  (swap! frame #(frame/register-subscription-handler % subscription-id handler-fn)))

(defn subscribe [subscription-spec]
  (scaffold/legacy-subscribe frame db subscription-spec))

(defn ^:export dispatch [job-id event]
  (put! event-chan [job-id event])
  nil)

(defn handle-event-and-report-exceptions [frame-atom db event]
  (try
    (frame/process-event-on-atom! @frame-atom db event)
    (catch :default e
      (error e (.-stack e)))))

(defn job-done [db [job-id]]
  (let [job (jobs/get-job job-id)
        coallesced-db (reagent/atom db)]
    (doseq [event (jobs/events job)]                                                                                  ; replay all buffered job events...
      (handle-event-and-report-exceptions frame coallesced-db event))
    (jobs/unregister-job job-id)
    (or ((jobs/continuation job) @coallesced-db) db)))                                                                ; contination can decide not to publish results (and maybe apply them later)

(register-handler :worker-job-done job-done)

(defn handle-event [frame db job-id event]
  (if (zero? job-id)
    (handle-event-and-report-exceptions frame db event)
    (jobs/buffer-job-event job-id event)))

(defn main-loop []
  (binding [plastic.env/*current-thread* "MAIN"]
    (go-loop []
      (let [[job-id event] (<! event-chan)]
        (handle-event frame db job-id event))
      (recur))))
