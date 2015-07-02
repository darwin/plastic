(ns quark.cogs.editor.utils
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.whitespace :as ws]
            [quark.cogs.editor.analyzer :refer [analyze-full]]
            [cljs.pprint :as pprint :refer [pprint]]
            [clojure.walk :refer [postwalk]]
            [clojure.zip :as z])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn make-zipper* [node]
  (z/zipper
    node/inner?
    (comp seq node/children)
    node/replace-children
    node))

(defn make-zipper [node]
  (if (= (node/tag node) :forms)
    (let [top (make-zipper* node)]
      (or (-> top zip/down ws/skip-whitespace) top))
    (recur (node/forms-node [node]))))

; interesting nodes are non-whitespace nodes and new lines
(defn node-interesting? [node]
  (or (node/linebreak? node) (not (node/whitespace? node))))

(def node-not-interesting? (complement node-interesting?))

(defn not-interesting? [loc]
  (node-not-interesting? (zip/node loc)))

; perform the given movement while the given predicate returns true
(defn skip [f p? loc]
  (first
    (drop-while #(if (or (z/end? %) (nil? %)) false (p? %))
      (iterate f loc))))

(defn skip-not-interesting [f loc]
  (skip f not-interesting? loc))

(defn skip-not-interesting-by-moving-left [loc]
  (skip-not-interesting z/left loc))

(defn skip-not-interesting-by-moving-right [loc]
  (skip-not-interesting z/right loc))

(defn move-right [loc]
  (some-> loc z/right skip-not-interesting-by-moving-right))

(defn move-left [loc]
  (some-> loc z/left skip-not-interesting-by-moving-left))

(defn move-down [loc]
  (some-> loc z/down skip-not-interesting-by-moving-right))

(defn move-up [loc]
  (some-> loc z/up skip-not-interesting-by-moving-left))

(defn move-next [loc]
  (some-> loc z/next skip-not-interesting-by-moving-right))

(defn leaf-nodes [loc]
  (filter (complement z/branch?)                            ; filter only non-branch nodes
    (take-while (complement z/end?)                         ; take until the :end
      (iterate move-next loc))))

(defn ancestor-count [loc]
  (dec (count (take-while move-up (iterate move-up loc)))))

(defn left-sibling-count [loc]
  (count (take-while move-left (iterate move-left loc))))

(defn root? [loc]
  (nil? (move-up loc)))

(defn make-rpath [loc]
  (let [parent-loc (move-up loc)]
    (if (root? parent-loc)
      []
      (conj (make-rpath parent-loc) (left-sibling-count loc)))))

(defn rpath->rloc [rpath rnode]
  (let [zloc (make-zipper rnode)]
    ))

(defn collect-all-right [loc]
  (take-while (complement zip/end?) (iterate zip/right loc)))

(defn- type-dispatcher [obj]
  (cond
    (fn? obj) :fn
    :default :default))

(defmulti clean-dispatch type-dispatcher)

(defmethod clean-dispatch :default [thing]
  (pprint/simple-dispatch thing))

(defmethod clean-dispatch :fn []
  (-write cljs.core/*out* "..."))

(defn print-node [node]
  (with-out-str
    (pprint/with-pprint-dispatch clean-dispatch
      (binding [pprint/*print-pretty* true
                pprint/*print-suppress-namespaces* true
                pprint/*print-lines* true]
        (pprint node)))))


