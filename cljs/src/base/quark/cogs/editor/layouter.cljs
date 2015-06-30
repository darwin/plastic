(ns quark.cogs.editor.layouter
  (:require [cljs.core.async :refer [<! timeout]]
            [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.whitespace :as ws]
            [quark.cogs.editor.analyzer :refer [analyze-full]]
            [clojure.walk :as walk]
            [clojure.zip :as z])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]]))

(defn edn* [node]
  (z/zipper
    node/inner?
    (comp seq node/children)
    node/replace-children
    node))

(defn make-zipper [node]
  (if (= (node/tag node) :forms)
    (let [top (edn* node)]
      (or (-> top zip/down ws/skip-whitespace) top))
    (recur (node/forms-node [node]))))

; interesting nodes are non-whitespace nodes and new lines
(defn node-interesting? [node]
  (or (node/linebreak? node) (not (node/whitespace? node))))

(def node-not-interesting? (complement node-interesting?))

(defn not-interesting? [loc]
  (node-not-interesting? (zip/node loc)))

; perform the given movement while the given predicate returns true
(defn skip [f p? zloc]
  (first
    (drop-while #(if (or (z/end? %) (nil? %)) false (p? %))
      (iterate f zloc))))

(defn skip-not-interesting [f zloc]
  (skip f not-interesting? zloc))

(defn skip-not-interesting-by-moving-left [zloc]
  (skip-not-interesting z/left zloc))

(defn skip-not-interesting-by-moving-right [zloc]
  (skip-not-interesting z/right zloc))

(defn move-right [zloc]
  (some-> zloc z/right skip-not-interesting-by-moving-right))

(defn move-left [zloc]
  (some-> zloc z/left skip-not-interesting-by-moving-left))

(defn move-down [zloc]
  (some-> zloc z/down skip-not-interesting-by-moving-right))

(defn move-up [zloc]
  (some-> zloc z/up skip-not-interesting-by-moving-left))

(defn move-next [zloc]
  (some-> zloc z/next skip-not-interesting-by-moving-right))

(defn leaf-nodes [loc]
  (filter (complement z/branch?)                            ; filter only non-branch nodes
    (take-while (complement z/end?)                         ; take until the :end
      (iterate move-next loc))))

(defn ancestor-count [loc]
  (dec (count (take-while move-up (iterate move-up loc)))))

(defn left-sibling-count [loc]
  (count (take-while move-left (iterate move-left loc))))

(defn get-path [loc]
  (let [parent (move-up loc)
        is-root? (not (boolean (move-up parent)))]
    (if is-root?
      []
      (conj (get-path parent) (left-sibling-count loc)))))

(defn item-info [loc]
  (let [node (zip/node loc)]
    {:string (zip/string loc)
     :tag    (node/tag node)
     :depth  (ancestor-count loc)
     :path   (get-path loc)}))

(defn build-soup [form]
  (map item-info (leaf-nodes (make-zipper form))))

(declare build-structure)

(defn build-structure [depth node]
  (merge {:tag   (node/tag node)
          :depth depth}
    (if (node/inner? node)
      {:children (doall (map (partial build-structure (inc depth)) (node/children node)))}
      {:text (node/string node)})))

(defn form-info [top]
  (let [node (zip/node top)]
    {:text      (zip/string top)
     :soup      (build-soup node)
     :structure (build-structure 0 node)}))

(defn walk-forms [res pos]
  (if (zip/end? pos)
    res
    (let [val (form-info pos)
          next (zip/right pos)]
      (recur (conj res val) next))))

(defn extract-top-level-forms [parsed]
  (let [top (make-zipper parsed)]
    (walk-forms [] top)))

(defn layout [editors [editor-id parsed]]
  (let [top-level-forms (extract-top-level-forms parsed)
        state {:forms top-level-forms}]
    (dispatch :editor-set-layout editor-id state)
    #_(go
      (<! (timeout 1000))                                   ; give it some time to render UI (next requestAnimationFrame)
      (dispatch :editor-analyze editor-id)))
  editors)

(defn set-layout [editors [editor-id state]]
  (assoc-in editors [editor-id :render-state] state))

(defn analyze [editors [editor-id]]
  (let [ast (analyze-full (get-in editors [editor-id :text]) {:atom-path (get-in editors [editor-id :def :uri])})]
    (log "AST>" ast))
  editors)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-layout paths/editors-path layout)
(register-handler :editor-set-layout paths/editors-path set-layout)
(register-handler :editor-analyze paths/editors-path analyze)