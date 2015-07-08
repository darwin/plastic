(ns quark.cogs.editor.layouter
  (:require [cljs.core.async :refer [<! timeout]]
            [rewrite-clj.zip :as zip]
            [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [quark.util.helpers :as helpers]
            [quark.cogs.editor.analysis.scopes :refer [analyze-scopes]]
            [quark.cogs.editor.analysis.symbols :refer [analyze-symbols]]
            [quark.cogs.editor.analysis.defs :refer [analyze-defs]]
            [quark.cogs.editor.layout.soup :refer [build-soup-render-info]]
            [quark.cogs.editor.layout.code :refer [build-code-render-info]]
            [quark.cogs.editor.layout.docs :refer [build-docs-render-info]]
            [quark.cogs.editor.layout.headers :refer [build-headers-render-info]]
            [quark.cogs.editor.layout.selections :refer [build-selections-render-info]]
            [quark.cogs.editor.analyzer :refer [analyze-full]]
            [quark.cogs.editor.utils :refer [debug-print-analysis ancestor-count loc->path leaf-nodes make-zipper collect-all-right]]
            [rewrite-clj.node :as node])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]]))

(defn extract-tokens [node]
  (if (= (:tag node) :token)
    [(:id node) node]
    (flatten (map extract-tokens (:children node)))))

(defn extract-selectables-from-code [node]
  (let [selectables-from-children (flatten (map extract-selectables-from-code (:children node)))]
    (if (:selectable node)
      (concat [(:id node) node] selectables-from-children)
      selectables-from-children)))

(defn extract-selectables-from-docs [docs-infos]
  (flatten (map (fn [info] [(:id info) info]) docs-infos)))

(defn collect-nodes [node]
  (let [relevant-nodes (fn [nodes] (remove #(or (node/whitespace? %) (node/comment? %)) nodes))
        child-nodes (fn [node]
                      (if (node/inner? node)
                        (flatten (map collect-nodes (relevant-nodes (node/children node))))))]
    (concat [(:id node) node] (child-nodes node))))

(defn prepare-form-render-info [loc]
  {:pre [(= (node/tag (zip/node (zip/up loc))) :forms)]}    ; parent has to be :forms
  (let [root-node (zip/node loc)
        _ (assert root-node)
        nodes (apply hash-map (collect-nodes root-node))
        analysis (->> {}
                   (analyze-scopes root-node)
                   (analyze-symbols nodes)
                   (analyze-defs root-node))
        code-info (build-code-render-info analysis loc)
        docs-info (build-docs-render-info analysis loc)
        headers-info (build-headers-render-info analysis loc)
        tokens (apply hash-map (extract-tokens code-info))
        selectables (apply hash-map (concat (extract-selectables-from-code code-info) (extract-selectables-from-docs docs-info)))]
    (debug-print-analysis root-node nodes analysis)
    {:id          (:id root-node)
     :nodes       nodes
     :analysis    analysis
     :tokens      tokens                                    ; will be used for soup generation
     :selectables selectables                               ; will be used for selections
     :skelet      {:text    (zip/string loc)                ; for plain text debug view
                   :code    code-info
                   :docs    docs-info
                   :headers headers-info}}))

(defn prepare-render-infos-of-top-level-forms [editor]
  (let [tree (:parse-tree editor)
        top (make-zipper tree)                              ; root "forms" node
        loc (zip/down top)                                  ; TODO: here we should skip possible whitespace
        right-siblinks (collect-all-right loc)              ; TODO: here we should use explicit zipping policy
        old-form-render-infos (get-in editor [:render-state :forms])
        find-old-form-render-info (fn [{:keys [id]}] (some #(if (= (:id %) id) %) old-form-render-infos))]
    (map #(merge (find-old-form-render-info (first %)) (prepare-form-render-info %)) right-siblinks)))

(defn analyze-with-delay [editor-id delay]
  (go
    (<! (timeout delay))                                    ; give it some time to render UI (next requestAnimationFrame)
    (dispatch :editor-analyze editor-id)))

(defn layout-editor [editor editor-id]
  (let [parse-tree (:parse-tree editor)
        render-infos (prepare-render-infos-of-top-level-forms editor)
        state {:forms      render-infos
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

(defn update-form-soup-geometry [form geometry]
  (assoc form :soup (build-soup-render-info (:tokens form) geometry)))

(defn update-soup-geometry [editors [editor-id form-id captured-layout]]
  (let [forms (get-in editors [editor-id :render-state :forms])
        new-forms (map #(if (= (:id %) form-id) (update-form-soup-geometry % captured-layout) %) forms)
        new-editors (assoc-in editors [editor-id :render-state :forms] new-forms)]
    new-editors))

(defn update-form-selectables-geometry [form geometry]
  (assoc form :all-selections (build-selections-render-info (:selectables form) geometry)))

(defn update-selectables-geometry [editors [editor-id form-id geometry]]
  (let [forms (get-in editors [editor-id :render-state :forms])
        new-forms (map #(if (= (:id %) form-id) (update-form-selectables-geometry % geometry) %) forms)
        new-editors (assoc-in editors [editor-id :render-state :forms] new-forms)]
    new-editors))

(defn analyze [editors [editor-id]]
  (let [ast (analyze-full (get-in editors [editor-id :text]) {:atom-path (get-in editors [editor-id :def :uri])})]
    (log "AST>" ast))
  editors)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-layout paths/editors-path layout)
(register-handler :editor-set-layout paths/editors-path set-layout)
(register-handler :editor-analyze paths/editors-path analyze)
(register-handler :editor-update-soup-geometry paths/editors-path update-soup-geometry)
(register-handler :editor-update-selectables-geometry paths/editors-path update-selectables-geometry)