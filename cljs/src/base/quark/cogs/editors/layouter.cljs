(ns quark.cogs.editors.layouter
  (:require [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [rewrite-clj.zip.whitespace :as ws]
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
        (log ">" val)
        (recur (conj res val) next)))))

(defn top-level-forms [parsed]
  (let [top (edn parsed)]
    (walk-forms [] top)))

(defn layout [editors [editor-id parsed]]
  (dispatch :editor-set-layout editor-id (top-level-forms parsed))
  editors)

(defn set-layout [editors [editor-id layout]]
  (assoc-in editors [editor-id :render-state :layout] layout))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-layout paths/editors-path layout)
(register-handler :editor-set-layout paths/editors-path set-layout)