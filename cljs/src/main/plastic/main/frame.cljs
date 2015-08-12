(ns plastic.main.frame
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [plastic.main.frame.core :as frame :refer [pure trim-v]]
            [plastic.main.frame.router :refer [event-chan purge-chan]]
            [clojure.string :as string]
            [plastic.main.frame.handlers :refer [handle register-base]]
            [cljs.core.async :refer [chan put! <!]]
            [reagent.ratom :as ratom]))

(def ^:dynamic *current-event-id* nil)
(defonce ^:dynamic effects (js-obj))

(defn register-after-effect [id fun]
  (aset effects id fun))

(defn unregister-after-effect [id]
  (aset effects id nil))

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
    (measure-time (str "MH! #" *current-event-id* " " (print-simple v))
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

(defn event-separator [db [id]]
  (or
    (if-let [after-effect (aget effects id)]
      (when-let [new-db-after-effect (after-effect db)]
        (unregister-after-effect id)
        new-db-after-effect))
    db))

(register-handler :worker-job-done event-separator)

(def subscribe frame/subscribe)

(defn process-event-and-silently-swallow-exceptions [event-v]
  (try
    (handle event-v)
    (catch :default _)))

(defn main-loop []
  (go-loop []
    (let [[id event-v] (<! event-chan)]
      (binding [*current-event-id* id]
        (process-event-and-silently-swallow-exceptions event-v))
      (recur))))

(defn dispatch [id event-v]
  (put! event-chan [id event-v])
  nil)