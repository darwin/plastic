(ns quark.cogs.editor.layouter
  (:require [cljs.core.async :refer [<! timeout]]
            [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [rewrite-clj.zip :as rzip]
            [rewrite-clj.node :as rnode]
            [quark.cogs.editor.analyzer :refer [analyze-full]]
            [quark.cogs.editor.utils :refer [leaf-nodes ancestor-count make-rpath make-zipper collect-all-right]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]]))

(defn item-info [zloc]
  (let [rnode (rzip/node zloc)]
    {:string (rzip/string zloc)
     :tag    (rnode/tag rnode)
     :depth  (ancestor-count zloc)
     :path   (make-rpath zloc)}))

(defn build-soup [rnode]
  (map item-info (leaf-nodes (make-zipper rnode))))

(declare build-structure)

(defn build-structure [depth rnode]
  (merge {:tag   (rnode/tag rnode)
          :depth depth}
    (if (rnode/inner? rnode)
      {:children (doall (map (partial build-structure (inc depth)) (rnode/children rnode)))}
      {:text (rnode/string rnode)})))

(defn form-info [zloc]
  (let [rnode (rzip/node zloc)]
    {:text      (rzip/string zloc)
     :soup      (build-soup rnode)
     :structure (build-structure 0 rnode)}))

(defn extract-top-level-form-infos [rnode]
  (let [zloc (make-zipper rnode)
        zlocs (collect-all-right zloc)]
    (doall (map form-info zlocs))))

(defn analyze-with-delay [editor-id delay]
  (go
    (<! (timeout delay))                                     ; give it some time to render UI (next requestAnimationFrame)
    (dispatch :editor-analyze editor-id)))

(defn layout [editors [editor-id]]
  (let [rnode (get-in editors [editor-id :parsed])
        top-level-forms (extract-top-level-form-infos rnode)
        state {:forms top-level-forms}]
    (dispatch :editor-set-layout editor-id state)
    #_(analyze-with-delay editor-id 1000))
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