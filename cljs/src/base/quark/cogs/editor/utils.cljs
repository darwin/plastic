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

(defn valid-loc? [loc]
  (not (or (nil? loc) (z/end? loc) (zip/end? loc))))        ; why is zip/end? doing it differently than z/end?

; perform the given movement while the given policy predicate returns true
(defn skip [movement policy loc]
  (first
    (drop-while #(and (valid-loc? %) (not (policy %)))
      (iterate movement loc))))

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

(defn leaf-nodes [policy loc]
  (filter (complement z/branch?)                            ; filter only non-branch nodes
    (take-while valid-loc?                                  ; take until the :end
      (iterate (partial zip-next policy) loc))))

(defn ancestor-count [policy loc]
  (dec (count (take-while valid-loc? (iterate (partial zip-up policy) loc)))))

(defn left-siblings-count [loc]
  (count (take-while z/left (iterate z/left loc))))

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

(def noop (fn [] []))

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

(defn unwrap-metas [nodes]
  (let [unwrap-meta-node (fn [node]
                           (if (= (node/tag node) :meta)
                             (unwrap-metas (node/children node))
                             [node]))]
    (mapcat unwrap-meta-node nodes)))