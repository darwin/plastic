(ns quark.util.helpers
  (:require [cljs.pprint :as pprint :refer [pprint]]
            [cljs.core.async :refer [put! <! chan timeout close!]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [cljs.core.async.macros :refer [go]]))

(defn deep-merge
  "Recursively merges maps. If keys are not maps, the last value wins."
  [& vals]
  (let [inputs (remove nil? vals)
        res (if (every? map? inputs)
              (apply merge-with deep-merge inputs)
              (last vals))]
    ;(log "DM" inputs "===>" res)
    res))

; --------------------------------------------------------
;

(defn- type-dispatcher [obj]
  (cond
    (fn? obj) :fn
    :default :default))

(defmulti clean-dispatch type-dispatcher)

(defmethod clean-dispatch :default [thing]
  (pprint/simple-dispatch thing))

(defmethod clean-dispatch :fn []
  (-write cljs.core/*out* "..."))

(defn nice-print [o]
  (with-out-str
    (pprint/with-pprint-dispatch clean-dispatch
      (binding [pprint/*print-pretty* true
                pprint/*print-suppress-namespaces* true
                pprint/*print-lines* true]
        (pprint o)))))

; --------------------------------------------------------

; underscore-like debounce
(defn debounce [f wait]
  (let [counter (atom 0)
        chan (atom (chan))]
    (fn [& args]
      (swap! counter inc)
      (let [snapshot @counter]
        (go
          (swap! chan (fn [c] (do (close! c) (timeout wait))))
          (<! @chan)
          (if (= snapshot @counter)
            (apply f args)))))))

(defn abs [x]
  (if (neg? x) (- x) x))