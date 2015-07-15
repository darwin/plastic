(ns plastic.util.helpers
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.pprint :as pprint :refer [pprint]]
            [cljs.core.async :refer [put! <! chan timeout close!]]
            [cuerdas.core :as str]))

(def noop (fn [] []))

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

(defn selector-match? [selector-spec val]
  (cond
    (vector? selector-spec) (some #{val} selector-spec)
    (set? selector-spec) (contains? selector-spec val)
    :default (= val selector-spec)))

(defn key-selector [k selector-spec]
  (fn [o]
    (let [key-val (get o k)]
      (selector-match? selector-spec key-val))))

(defn update-selected [selector f coll]
  (map #(if (selector %) (f %) %) coll))

(defn get-by-key [key val coll]
  (some #(if (= (key %) val) %) coll))

(def get-by-id (partial get-by-key :id))

(defn next-item [f coll]
  (nth (drop-while #(not (f %)) coll) 1 nil))

(defn prev-item [f coll]
  (last (take-while #(not (f %)) coll)))

(defn strip-colon [text]
  (str/ltrim text ":"))                                     ; TODO: this must be more robust

