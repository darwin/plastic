(ns plastic.util.helpers
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [cljs.core.async.macros :refer [go]])
  (:require [cljs.pprint :as pprint :refer [pprint]]
            [cljs.core.async :refer [put! <! chan timeout close!]]
            [clojure.set :as set]
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

#_(defmethod clean-dispatch :fn []
  (-write *out* "..."))

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

(defn selector-matches? [selector id]
  (cond
    (nil? selector) true
    (vector? selector) (some #{id} selector)
    (set? selector) (contains? selector id)
    :default (= id selector)))


; We have to be extra careful when updating data observed by reagent's reactions.
; For performance reasons reactions do identical? tests to detect changes.
; See discussion https://github.com/reagent-project/reagent/pull/143
;
; We may want to run a code which constructs same values which do not turn to be indentical.
; For example, imagine editor's layouting code running after some trivial change in code tree structure.
; The code will produce almost same layouting data strucutre, but individual nodes
; in the structure won't be indentical to the old versions (although many will be equal).
; => this triggers re-rendeing of whole tree of reagent components
;
; To avoid unnecessary rendering we split our layouting results into bite-sized chunks
; (in case of layouting we keep per-node layouting data key-ed by node-id) and put them into maps.
; Then when commiting new layouting results, we use overwrite-map to do equality check on individual pieces
; and replace old value only if there is any change.
;
; A similar strategy should be applied everywhere where we are touching individual
; data pieces observed by reagent's reactions (and re-frame subscriptions).
;
(defn overwrite-map [old-map new-map]
  {:pre [(or (nil? old-map) (map? old-map))
         (map? new-map)]}
  (if (nil? old-map)
    new-map
    (let [new-keys (set (keys new-map))
          old-keys (set (keys old-map))
          keys-to-be-removed (set/difference old-keys new-keys)
          keys-to-be-added (set/difference new-keys old-keys)
          keys-to-be-updated (set/intersection old-keys new-keys)
          careful-updater (fn [accum k]
                            (update accum k (fn [old-val]
                                              (let [new-val (get new-map k)]
                                                (if (= old-val new-val)
                                                  old-val
                                                  new-val)))))
          simple-adder (fn [accum k] (assoc accum k (get new-map k)))
          old-map-after-removal (apply dissoc old-map keys-to-be-removed)
          old-map-after-removal-and-update (reduce careful-updater old-map-after-removal keys-to-be-updated)]
      (reduce simple-adder old-map-after-removal-and-update keys-to-be-added))))

(defn indexed-iteration [coll]
  {:pre [(coll? coll)]}
  (map-indexed (fn [i v] [i v]) coll))