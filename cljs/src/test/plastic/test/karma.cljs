(ns plastic.test.karma
  (:require-macros [plastic.logging :refer [log info warn error group group-end fancy-log]]
                   [plastic.test.karma :refer [number-of-tests-in-all-namespaces]])
  (:require [cljs.test :refer-macros [run-all-tests run-tests]]
            [plastic.suites.all-tests]))

; inspired by https://github.com/honzabrecka/karma-reporter

(def *test-var-state* (atom {}))

; -------------------------------------------------------------------------------------------------------------------

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

(defn present-records [record]
  (str
    "  failed: " (cljs.test/testing-vars-str record) "\n"
    "expected: " (pr-str (:expected record)) "\n"
    "  actual: " (pr-str (:actual record)) "\n"))

(defn env-with-default-reporter []
  (assoc (cljs.test/get-current-env) :reporter :cljs.test/default))

(defn call-default-reporter [record]
  (binding [cljs.test/*current-env* (env-with-default-reporter)]
    (cljs.test/report record)))

(defn add-test-record [state collection record]
  (swap! state update-in [:collections collection] conj record))

; -------------------------------------------------------------------------------------------------------------------

(defmethod cljs.test/report [::karma :begin-test-var] [record]
  (reset! *test-var-state*
    {:start-time  (now)
     :collections {:fail  []
                   :error []
                   :pass  []}})
  (call-default-reporter record))

(defmethod cljs.test/report [::karma :end-test-var] [record]
  (let [{:keys [ns name]} (meta (:var record))
        {:keys [start-time collections]} @*test-var-state*
        time-delta (- (now) start-time)
        errors-and-failures (get-errors-and-failures collections)]
    (karma-result! {"suite"       [ns]
                    "description" name
                    "success"     (empty? errors-and-failures)
                    "skipped"     nil
                    "time"        time-delta
                    "log"         (map present-records errors-and-failures)}))
  (call-default-reporter record))

(defmethod cljs.test/report [::karma :pass] [record]
  (add-test-record *test-var-state* :pass record)
  (call-default-reporter record))

(defmethod cljs.test/report [::karma :fail] [record]
  (add-test-record *test-var-state* :fail record)
  (call-default-reporter record))

(defmethod cljs.test/report [::karma :error] [record]
  (add-test-record *test-var-state* :error record)
  (call-default-reporter record))

(defmethod cljs.test/report [::karma :end-run-tests] [record]
  (call-default-reporter record)
  (karma-complete!))

; -------------------------------------------------------------------------------------------------------------------

(defn run-all-tests! []
  (enable-console-print!)
  (let [test-env (cljs.test/empty-env ::karma)]
    (karma-info! {:total (number-of-tests-in-all-namespaces)})
    (run-all-tests nil test-env)))