(ns plastic.reagent.sonar
  (:require-macros [plastic.logging :refer [log info warn error group group-end measure-time]])
  (:require [reagent.ratom :as ratom]
            [plastic.util.helpers :as helpers]
            [clojure.data :as data]))

; -------------------------------------------------------------------------------------------------------------------
; Sonars - pools of lightweight path-aware reactions
; for rationale see https://github.com/reagent-project/reagent/issues/165

(defn count-reactions [paths-tree]
  (reduce (fn [sum [_ val]]
            (if (= val ::reaction)
              (inc sum)
              (+ sum (count-reactions val)))) 0 paths-tree))

(defn debug-sonars? [context]
  (get-in context [:env :config :debug-sonars]))

(defn bench-sonars? [context]
  (get-in context [:env :config :bench-sonars]))

; this is the main optimization
; when ratom changes, sonar's -handle-change is called
; this method is called to prune the tree of paths and run only reactions on paths affected by changes
; we use identical? check to keep this fast and try reject whole branches as soon as possible
; see discussion: https://github.com/reagent-project/reagent/pull/143
(defn match-paths [context old-data new-data paths-tree]
  (let [debug-sonars (debug-sonars? context)]
    (doseq [[key val] paths-tree]
      (if (= val ::reaction)
        (do
          (if debug-sonars (log "triggering" key))
          (ratom/run key))                                                                                            ; in case of ::reaction stopper, the key is actual SonarReaction instance
        (let [old (get old-data key)
              new (get new-data key)]
          (if-not (identical? old new)
            (match-paths context old new val)))))))

(declare destroy-sonar!)

; -------------------------------------------------------------------------------------------------------------------

(defprotocol ISonar)

(defprotocol ISonarPeek
  (-peek-at-path [this path]))

(defprotocol ISonarFilter
  (-handle-change [this sender oldval newval]))

(defprotocol ISonarRegistration
  (-register [this sonar-reaction])
  (-unregister [this sonar-reaction]))

(defprotocol ISonarWatching
  (-start-watching [this])
  (-stop-watching [this]))

(defprotocol ISonarDispose
  (-dispose [this]))

(deftype Sonar [context source ^:mutable paths-tree]

  IHash
  (-hash [this] (goog/getUid this))

  ISonar

  ISonarPeek
  (-peek-at-path [_this path]
    (binding [ratom/*ratom-context* nil]
      (get-in @source path)))

  ISonarDispose
  (-dispose [_this]
    (destroy-sonar! context source))

  ISonarFilter
  (-handle-change [this _sender old-data new-data]
    (measure-time (bench-sonars? context) "SONAR" [(str "#" (hash this))]
      (if (debug-sonars? context)
        (log "handle-change paths-tree:" paths-tree "diff:" (data/diff old-data new-data)))
      (match-paths context old-data new-data paths-tree)))

  ISonarWatching
  (-start-watching [this]
    (add-watch source this -handle-change))
  (-stop-watching [this]
    (remove-watch source this))

  ISonarRegistration
  (-register [this sonar-reaction]
    {:pre [(nil? (get paths-tree (conj (.-path sonar-reaction) sonar-reaction)))]}
    (if (empty? paths-tree)
      (-start-watching this))
    (set! paths-tree (assoc-in paths-tree (conj (.-path sonar-reaction) sonar-reaction) ::reaction))
    (if (debug-sonars? context)
      (log "registered" sonar-reaction "in" this)))
  (-unregister [this sonar-reaction]
    {:pre [(get paths-tree (conj (.-path sonar-reaction) sonar-reaction))]}
    (set! paths-tree (helpers/dissoc-in paths-tree (conj (.-path sonar-reaction) sonar-reaction)))
    (if (debug-sonars? context)
      (log "unregistered" sonar-reaction "from" this))
    (when (empty? paths-tree)
      (-stop-watching this)
      (-dispose this)))

  IPrintWithWriter
  (-pr-writer [this writer opts]
    (-write writer (str "#<Sonar #" (hash this) " "))
    (-write writer (str (count-reactions (.-paths-tree this)) " reactions | "))
    (pr-writer (.-paths-tree this) writer opts)
    (-write writer " <~~ ")
    (pr-writer source writer opts)
    (-write writer ">")))

(defn make-sonar [context ratom]
  (Sonar. context ratom nil))

; -------------------------------------------------------------------------------------------------------------------

(deftype SonarReaction [sonar path ^:mutable state ^:mutable watches]

  IAtom

  IHash
  (-hash [this] (goog/getUid this))

  IEquiv
  (-equiv [this other] (identical? this other))

  IPrintWithWriter
  (-pr-writer [this writer opts]
    (-write writer (str "#<SonarReaction #" (hash this) " :"))
    (pr-writer path writer opts)
    (-write writer " | ")
    (pr-writer state writer opts)
    (-write writer ">"))

  IWatchable
  (-notify-watches [this oldval newval]
    (doseq [[key f] watches]
      (f key this oldval newval)))
  (-add-watch [this key f]
    (set! watches (assoc watches key f))
    this)
  (-remove-watch [this key]
    (set! watches (dissoc watches key))
    (when (empty? watches)
      (ratom/dispose! this)))

  IDeref
  (-deref [this]
    (ratom/notify-deref-watcher! this)
    (if (= state ::nil)
      (set! state (-peek-at-path sonar path)))
    state)

  ratom/IReactiveAtom

  ratom/IRunnable
  (run [this]
    (let [old-state state
          new-state (-peek-at-path sonar path)]
      (set! state new-state)
      (-notify-watches this old-state new-state)))

  ratom/IDisposable
  (dispose! [this]
    (-unregister sonar this)))

(defn make-sonar-reaction [sonar path]
  {:pre [(vector? path)]}
  (let [reaction (SonarReaction. sonar path ::nil nil)]
    (-register sonar reaction)
    reaction))

; -------------------------------------------------------------------------------------------------------------------

(defn init-sonar-pool [context]
  {:pre [(get context :env)]}
  (-> context
    (assoc :sonars (atom {}))))

(defn clean-sonar-pool [context]
  (-> context
    (dissoc :sonars)))

(defn create-sonar! [context source]
  {:pre [(nil? (get @(:sonars context) source))]}
  (let [sonar (make-sonar context source)]
    (swap! (:sonars context) assoc source sonar)
    (if (debug-sonars? context)
      (log "created " sonar))
    sonar))

(defn get-or-create-sonar! [context source]
  {:post [(satisfies? ISonar %)]}
  (or (get @(:sonars context) source) (create-sonar! context source)))

(defn destroy-sonar! [context source]
  {:pre [(get @(:sonars context) source)]}
  (if (debug-sonars? context)
    (log "destroyed " (get @(:sonars context) source)))
  (swap! (:sonars context) dissoc source))