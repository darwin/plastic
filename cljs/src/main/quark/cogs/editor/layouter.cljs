(ns quark.cogs.editor.layouter
  (:require [cljs.core.async :refer [<! timeout]]
            [rewrite-clj.zip :as zip]
            [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [quark.cogs.editor.analysis.scopes :refer [analyze-scopes]]
            [quark.cogs.editor.analysis.symbols :refer [analyze-symbols]]
            [quark.cogs.editor.analysis.defs :refer [analyze-defs]]
            [quark.cogs.editor.analysis.cursors :refer [analyze-cursors]]
            [quark.cogs.editor.layout.soup :refer [build-soup-render-info]]
            [quark.cogs.editor.layout.code :refer [build-code-render-info]]
            [quark.cogs.editor.layout.docs :refer [build-docs-render-info]]
            [quark.cogs.editor.layout.headers :refer [build-headers-render-info]]
            [quark.cogs.editor.analyzer :refer [analyze-full]]
            [quark.cogs.editor.utils :refer [debug-print-analysis ancestor-count loc->path leaf-nodes make-zipper collect-all-right]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]]))

(defn form-layout-info [editor loc]
  (let [node (zip/node loc)
        analysis (->> {}
                   (analyze-scopes node)
                   (analyze-symbols node)
                   (analyze-defs node)
                   (analyze-cursors editor))]
    (debug-print-analysis node analysis)
    {:id       (:id node)
     :text     (zip/string loc)
     :analysis analysis
     :skelet   {:code    (build-code-render-info analysis loc)
                :docs    (build-docs-render-info analysis loc)
                :headers (build-headers-render-info analysis loc)}}))

(defn prepare-top-level-forms [editor]
  (let [node (get editor :parse-tree)
        top (make-zipper node)                              ; root "forms" node
        loc (zip/down top)
        right-siblinks (collect-all-right loc)]
    (map (partial form-layout-info editor) right-siblinks)))

(defn analyze-with-delay [editor-id delay]
  (go
    (<! (timeout delay))                                    ; give it some time to render UI (next requestAnimationFrame)
    (dispatch :editor-analyze editor-id)))

(defn layout-editor [editor editor-id]
  (let [parse-tree (get editor :parse-tree)
        top-level-forms (prepare-top-level-forms editor)
        state {:forms      top-level-forms
               :parse-tree parse-tree}]
    (dispatch :editor-set-layout editor-id state)
    #_(analyze-with-delay editor-id 1000))
  editor)

(defn for-each-editor [editors f]
  (apply array-map
    (flatten
      (for [[editor-id editor] editors]
        [editor-id (f editor editor-id)]))))

(defn layout [editors [editor-id]]
  (if editor-id
    (assoc editors editor-id (layout-editor (get editors editor-id) editor-id))
    (for-each-editor editors layout-editor)))

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