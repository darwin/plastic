(ns plastic.util.helpers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [plastic.logging :refer-macros [log info warn error group group-end]]
            [plastic.common :refer-macros [process]]
            [cljs.pprint :as pprint :refer [pprint]]
            [cljs.core.async :refer [put! <! chan timeout close!]]
            [goog.object :as gobj]
            [clojure.set :as set]
            [cuerdas.core :as str]
            [clojure.string :as string]))

; -------------------------------------------------------------------------------------------------------------------

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

; -------------------------------------------------------------------------------------------------------------------

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

; -------------------------------------------------------------------------------------------------------------------

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
  (let [tail (drop-while #(not (f %)) coll)]
    (if (empty? tail)
      (first coll)
      (second tail))))

(defn prev-item [f coll]
  (next-item f (reverse coll)))

(defn strip-colon [text]
  (if (= (first text) ":")
    (.substring text 1)
    text))

(defn strip-semicolor-and-whitespace-left [text]
  (.replace text #"^[;]+[\s\t]?" ""))

(defn count-lines [text]
  (.-length (.split text "\n")))

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
(defn overwrite-map
  ([old-map new-map] (overwrite-map old-map new-map nil))
  ([old-map new-map construct]
   {:pre [(or (nil? old-map) (map? old-map))
          (map? new-map)]}
   (if (nil? old-map)
     (if construct
       (into (construct) new-map)
       new-map)
     (let [new-keys (set (keys new-map))
           old-keys (set (keys old-map))
           keys-to-be-removed (set/difference old-keys new-keys)
           keys-to-be-added (set/difference new-keys old-keys)
           keys-to-be-updated (set/intersection old-keys new-keys)
           careful-updater (fn [accum k]
                             (update accum k (fn [old-val]
                                               (let [new-val (get new-map k)]
                                                 (if (= old-val new-val)                                              ; deep equality test
                                                   old-val
                                                   new-val)))))
           simple-adder (fn [accum k] (assoc accum k (get new-map k)))
           old-map-after-removal (apply dissoc old-map keys-to-be-removed)
           old-map-after-removal-and-update (reduce careful-updater old-map-after-removal keys-to-be-updated)]
       (reduce simple-adder old-map-after-removal-and-update keys-to-be-added)))))

; we can also do overwrite-map in two steps:
; prepare map patch with prepare-map-patch
; and apply the patch later with patch-map call
; this is usefull for transmitting only patches over some wire
(defn prepare-map-patch
  ([old-map new-map]
   {:pre [(or (nil? old-map) (map? old-map))
          (map? new-map)]}
   (let [new-keys (set (keys new-map))
         old-keys (set (keys old-map))
         keys-to-be-removed (set/difference old-keys new-keys)
         keys-to-be-added (set/difference new-keys old-keys)
         keys-to-be-updated (set/intersection old-keys new-keys)
         collect-modified (fn [accum k]
                            (let [old-val (get old-map k)
                                  new-val (get new-map k)]
                              (if-not (= old-val new-val)                                                             ; deep equality test
                                (conj accum [k new-val])
                                accum)))
         modified (reduce collect-modified [] keys-to-be-updated)
         added (map (fn [k] [k (get new-map k)]) keys-to-be-added)]
     [keys-to-be-removed (concat modified added)])))

(defn patch-map
  ([old-map map-patch] (patch-map old-map map-patch hash-map))
  ([old-map [keys-for-removal modifications] construct]
   {:pre [(or (nil? old-map) (map? old-map))]}
   (let [map-after-removal (if old-map (apply dissoc old-map keys-for-removal) (construct))]
     (into map-after-removal modifications))))

(defn indexed-iteration [coll]
  {:pre [(coll? coll)]}
  (map-indexed (fn [i v] [i v]) coll))

; -------------------------------------------------------------------------------------------------------------------
; scgilardi at gmail
; taken from https://github.com/clojure/core.incubator/blob/master/src/main/clojure/clojure/core/incubator.clj

(defn dissoc-in
  "Dissociates an entry from a nested associative structure returning a new
  nested structure. keys is a sequence of keys. Any empty maps that result
  will not be present in the new structure."
  [m [k & ks :as keys]]
  (if ks
    (if-let [nextmap (get m k)]
      (let [newmap (dissoc-in nextmap ks)]
        (if (seq newmap)
          (assoc m k newmap)
          (dissoc m k)))
      m)
    (dissoc m k)))

; https://gist.github.com/ptaoussanis/2556c56d93bde4af0415
(defn oget
  "Like `aget` for JS objects, Ref. https://goo.gl/eze8hY. Unlike `aget`,
  returns nil for missing keys instead of throwing."
  ([o k] (when o (gobj/get o k nil)))
  ([o k1 k2] (when-let [o (oget o k1)] (gobj/get o k2 nil)))                                                          ; Optimized common case
  ([o k1 k2 & ks] (when-let [o (oget o k1 k2)] (apply oget o ks))))                                                   ; Can also lean on optimized 2-case

(defn remove-nil-values [m]
  (process (keys m) m
    (fn [accum key]
      (if (nil? (get m key))
        (dissoc accum key)
        accum))))

(defn process-map
  ([m f] (process-map m f {}))
  ([m f init]
   {:pre [(map? m)]}
   (reduce-kv #(assoc %1 %2 (f %3)) init m)))

(defn best-val [s op]
  (process s ::nil
    (fn [best val]
      (if (or (= best ::nil) (op val best))
        val
        best))))

(defn select-values [pred map]
  (let [keys (keep (fn [[k v]] (if (pred v) k)) map)]
    (select-keys map keys)))

(defn convert-from-js [x]
  (let [keyfn (fn [k] (keyword (string/replace k "_" "-")))]
    (into {} (for [k (js-keys x)]
               [(keyfn k) (aget x k)]))))

(defn vupdate! [v f & args]
  (vreset! v (apply f @v args)))