(ns plastic.reagent.sonar
  (:refer-clojure :exclude [atom])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end measure-time]]
                   reagent.ratom)
  (:require [reagent.ratom :as ratom]
            [plastic.util.helpers :as helpers]))

(defonce ^:mutable sonars {})

(declare destroy-sonar)

(defprotocol ISonarGet
  (-get [this path]))

(defprotocol ISonarFilter
  (-handle-change [this sender oldval newval]))

(defprotocol ISonarRegistration
  (-register [this path sonar-reaction])
  (-unregister [this path sonar-reaction]))

(defprotocol ISonarWatching
  (-start-watching [this])
  (-stop-watching [this]))

(defprotocol ISonarDispose
  (-dispose [this]))

(defn match-paths [oldval newval paths]
  (doseq [[k v] paths]
    (if (= v ::reaction)
      (ratom/run k)
      (let [old (get oldval k)
            new (get newval k)]
        (if-not (identical? old new)
          (match-paths old new v))))))

(deftype Sonar [source ^:mutable paths]

  ISonarGet
  (-get [_this path]
    (binding [ratom/*ratom-context* nil]
      (get-in @source path)))

  ISonarDispose
  (-dispose [_this]
    (destroy-sonar source))

  ISonarFilter
  (-handle-change [_this _sender oldval newval]
    (measure-time (str "S! ")
      (match-paths oldval newval paths)))

  ISonarWatching
  (-start-watching [this]
    (add-watch source this -handle-change))
  (-stop-watching [this]
    (remove-watch source this))

  ISonarRegistration
  (-register [this path sonar-reaction]
    {:pre [(nil? (get paths (conj path sonar-reaction)))]}
    (if (empty? paths)
      (-start-watching this))
    (set! paths (assoc-in paths (conj path sonar-reaction) ::reaction)))
  (-unregister [this path sonar-reaction]
    {:pre [(get paths (conj path sonar-reaction))]}
    (set! paths (helpers/dissoc-in paths (conj path sonar-reaction)))
    (when (empty? paths)
      (-stop-watching this)
      (-dispose this)))

  IPrintWithWriter
  (-pr-writer [_o writer opts]
    (-write writer "#<Sonar:")
    (pr-writer paths writer opts)
    (-write writer ">")))

(defn make-sonar [ratom]
  (Sonar. ratom {}))

(deftype SonarReaction [sonar path ^:mutable state ^:mutable watches]

  IAtom

  IHash
  (-hash [this] (goog/getUid this))

  IEquiv
  (-equiv [o other] (identical? o other))

  IPrintWithWriter
  (-pr-writer [this writer opts]
    (-write writer (str "#<SonarReaction " (hash this) " " path ": "))
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
      (set! state (-get sonar path)))
    state)

  ratom/IReactiveAtom

  ratom/IRunnable
  (run [this]
    (let [old-state state
          new-state (-get sonar path)]
      (set! state new-state)
      (-notify-watches this old-state new-state)))

  ratom/IDisposable
  (dispose! [this]
    (-unregister sonar path this)))

(defn make-sonar-reaction [sonar path]
  {:pre [(vector? path)]}
  (if (= path [:editors])
    (js-debugger))
  (let [reaction (SonarReaction. sonar path ::nil nil)]
    (-register sonar path reaction)
    reaction))

(defn create-sonar [source]
  {:pre [(nil? (get sonars source))]}
  (let [sonar (make-sonar source)]
    (set! sonars (assoc sonars source sonar))
    (log "created sonar" sonar "for" source)
    sonar))

(defn get-or-create-sonar! [source]
  (or (get sonars source) (create-sonar source)))

(defn destroy-sonar [source]
  {:pre [(get sonars source)]}
  (log "destroyed sonar" (get sonars source) "for" source)
  (set! sonars (dissoc sonars source)))