(ns quark.cogs.editor.layouter
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.whitespace :as ws]
            [quark.cogs.editor.analyzer :refer [analyze-file]]
            [clojure.zip :as z])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]))

(defn edn* [node]
  (z/zipper
    node/inner?
    (comp seq node/children)
    node/replace-children
    node))

(defn edn [node]
  (if (= (node/tag node) :forms)
    (let [top (edn* node)]
      (or (-> top zip/down ws/skip-whitespace) top))
    (recur (node/forms-node [node]))))

(defn walk-forms [res pos]
  (if (zip/end? pos)
    res
    (do
      (let [val (zip/string pos)
            next (zip/right pos)]
        (recur (conj res val) next)))))

(defn extract-top-level-forms [parsed]
  (let [top (edn parsed)]
    (walk-forms [] top)))

(defn layout [editors [editor-id parsed]]
  (let [top-level-forms (extract-top-level-forms parsed)
        asts (try
               (analyze-file (get-in editors [editor-id :text]) {:atom-path (get-in editors [editor-id :def :uri])})
               (catch js/Error e
                 (str (.-message e) "\n" (.-stack e))))]
    (log "AST>" asts)
    (dispatch :editor-set-layout editor-id top-level-forms))
  editors)

(defn set-layout [editors [editor-id layout]]
  (assoc-in editors [editor-id :render-state :layout] layout))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-layout paths/editors-path layout)
(register-handler :editor-set-layout paths/editors-path set-layout)