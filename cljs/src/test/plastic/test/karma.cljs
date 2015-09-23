(ns plastic.test.karma
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.test.karma :refer [total-number-of-tests-in-all-namespaces]])
  (:require [cljs.test :refer-macros [run-all-tests run-tests]]))

; inspired by https://github.com/honzabrecka/karma-reporter

(defn karma-present? []
  (not (nil? js/__karma__)))

(defn call-karma [method args]
  (if (karma-present?)
    (.apply (aget js/__karma__ method) js/__karma__ (into-array (map clj->js args)))))

(defn- karma-info! [& args]
  (call-karma "info" args))

(defn- karma-result! [& args]
  (call-karma "result" args))

(defn- karma-complete! [& args]
  (call-karma "complete" args))

; -------------------------------------------------------------------------------------------------------------------

(defn- now []
  (.getTime (js/Date.)))

(defn get-errors-and-failures [collections]
  (concat (:error collections) (:fail collections)))

(defn build-karma-log-entry [record]
  (str
    "  failed: " (cljs.test/testing-vars-str record) "\n"
    "expected: " (pr-str (:expected record)) "\n"
    "  actual: " (pr-str (:actual record))) "\n")

(defn build-karma-log [records]
  (keep build-karma-log-entry records))

(defn call-default-reporter [record]
  (cljs.test/update-current-env! [:reporter] (constantly :cljs.test/default))
  (try
    (cljs.test/report record)
    (finally
      (cljs.test/update-current-env! [:reporter] (constantly ::karma)))))

(defn get-karma-state []
  (::karma-state (cljs.test/get-current-env)))

(def karma-state-updater! (partial cljs.test/update-current-env! [::karma-state]))

(defn update-karma-state! [f & args]
  (apply karma-state-updater! f args))

(defn set-karma-state! [new-state]
  (karma-state-updater! identity new-state))

(defn add-test-record [collection record]
  (update-karma-state! update-in [:collections collection] conj record))

; -------------------------------------------------------------------------------------------------------------------

(defmethod cljs.test/report [::karma :begin-test-var] [record]
  (set-karma-state! {:start-time  (now)
                     :collections {:fail  []
                                   :error []
                                   :pass  []}})
  (call-default-reporter record))

(defmethod cljs.test/report [::karma :end-test-var] [record]
  (let [{:keys [ns name]} (meta (:var record))
        {:keys [start-time collections]} (get-karma-state)
        time-delta (- (now) start-time)
        errors-and-failures (get-errors-and-failures collections)]
    (karma-result! {"suite"       [ns]
                    "description" name
                    "success"     (empty? errors-and-failures)
                    "skipped"     nil
                    "time"        time-delta
                    "log"         (build-karma-log errors-and-failures)}))
  (call-default-reporter record))

(defmethod cljs.test/report [::karma :pass] [record]
  (add-test-record :pass record)
  (call-default-reporter record))

(defmethod cljs.test/report [::karma :fail] [record]
  (add-test-record :fail record)
  (call-default-reporter record))

(defmethod cljs.test/report [::karma :error] [record]
  (add-test-record :error record)
  (call-default-reporter record))

(defmethod cljs.test/report [::karma :end-run-tests] [record]
  (call-default-reporter record)
  (karma-complete!))

; -------------------------------------------------------------------------------------------------------------------

(defn run-all-tests! []
  (enable-console-print!)
  (let [test-env (cljs.test/empty-env ::karma)]
    (karma-info! {:total (total-number-of-tests-in-all-namespaces)})
    (run-all-tests nil test-env)))

(defn setup! []
  (aset js/__karma__ "start" run-all-tests!))