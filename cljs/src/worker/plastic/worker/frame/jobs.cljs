(ns plastic.worker.frame.jobs
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]))

(defonce ^:dynamic *pending-jobs-counters* {})
(defonce ^:dynamic *pending-jobs-initial-dbs* {})

(defn get-initial-db-for-job [job-id]
  (get *pending-jobs-initial-dbs* job-id))

(defn set-initial-db-for-job! [job-id db]
  (if-not (get-initial-db-for-job job-id)
    (set! *pending-jobs-initial-dbs* (assoc *pending-jobs-initial-dbs* job-id db))))

(defn inc-counter-for-job! [job-id]
  (set! *pending-jobs-counters* (update *pending-jobs-counters* job-id inc)))

(defn dec-counter-for-job! [job-id]
  (set! *pending-jobs-counters* (update *pending-jobs-counters* job-id dec))
  (get *pending-jobs-counters* job-id))

(defn remove-counter-for-job! [job-id]
  (set! *pending-jobs-counters* (dissoc *pending-jobs-counters* job-id)))

(defn pop-initial-db-for-job! [job-id]
  (let [initial-db (get-initial-db-for-job job-id)]
    (assert initial-db)
    (set! *pending-jobs-initial-dbs* (dissoc *pending-jobs-initial-dbs* job-id))
    initial-db))

(defn update-counter-for-job-and-pop-initial-db-if-finished! [job-id]
  (when (zero? (dec-counter-for-job! job-id))
    (remove-counter-for-job! job-id)
    (pop-initial-db-for-job! job-id)))
