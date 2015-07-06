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
    (make-zipper* node)
    (recur (node/forms-node [node]))))

; interesting nodes are non-whitespace nodes and new lines
(defn node-interesting? [node]
  (or (node/linebreak? node) (not (node/whitespace? node))))

(defn loc-interesting? [loc]
  (node-interesting? (zip/node loc)))

(defn layouting-children [node]
  (filter node-interesting? (node/children node)))

(defn layouting-children-zip [loc]
  (let [first (zip/down loc)
        children (take-while (complement nil?)
                   (iterate z/right first))]
    (filter #(node-interesting? (zip/node %)) children)))

(defn essential-nodes [nodes]
  (filter #(not (or (node/whitespace? %) (node/comment? %))) nodes))

(defn essential-children [node]
  (essential-nodes (node/children node)))

(defn valid-loc? [loc]
  (not (or (nil? loc) (z/end? loc) (zip/end? loc))))

; perform the given movement while the given policy predicate returns true
(defn skip [movement policy loc]
  (first
    (drop-while #(and (valid-loc? %) (not (policy %)))
      (iterate movement loc))))

(defn skip-not-interesting [f loc]
  (skip f loc-interesting? loc))

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

(defn zip-right [policy loc]
  (some->> loc z/right (skip z/right policy)))

(defn zip-left [policy loc]
  (some->> loc z/left (skip z/left policy)))

(defn zip-down [policy loc]
  (some->> loc z/down (skip z/right policy)))

(defn zip-up [policy loc]
  (some->> loc z/up (skip z/left policy)))

(defn zip-next [policy loc]
  (some->> loc z/next (skip z/right policy)))

(defn leaf-nodes [loc]
  (filter (complement z/branch?)                            ; filter only non-branch nodes
    (take-while (complement z/end?)                         ; take until the :end
      (iterate move-next loc))))

(defn ancestor-count [loc]
  (dec (count (take-while move-up (iterate move-up loc)))))

(defn left-siblings-count [loc]
  (count (take-while z/left (iterate z/left loc))))

(defn root? [loc]
  (nil? (move-up loc)))

(defn loc->path [loc]
  (let [parent-loc (z/up loc)
        pos (left-siblings-count loc)]
    (if parent-loc
      (conj (loc->path parent-loc) pos)
      [])))

(defn path->loc [path loc]
  (if (and path (valid-loc? loc))
    (if (empty? path)
      loc
      (let [down-loc (z/down loc)
            child-loc (nth (iterate z/right down-loc) (first path))]
        (if (valid-loc? child-loc)
          (recur (rest path) child-loc))))))

(defn collect-all-right [loc]
  (take-while (complement zip/end?) (iterate zip/right loc)))

(defn collect-all-parents [loc]
  (take-while valid-loc? (iterate z/up loc)))

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

(defn node-walker [inner-fn leaf-fn reducer child-selector]
  (let [walker (fn walk [node]
                 (if (node/inner? node)
                   (let [node-results (inner-fn node)
                         children-results (mapcat walk (child-selector node))
                         results (apply reducer (concat children-results node-results))]
                     [results])
                   (leaf-fn node)))]
    (fn [node]
      (apply reducer (walker node)))))

(defn node-children-unwrap-metas [node]
  (let [children (node/children node)
        unwrap-meta-node (fn [node]
                           (if (= (node/tag node) :meta)
                             (node-children-unwrap-metas node)
                             [node]))]
    (mapcat unwrap-meta-node children)))
