(ns quark.cogs.editor.layouter
  (:require [cljs.core.async :refer [<! timeout]]
            [rewrite-clj.zip :as zip]
            [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [quark.cogs.editor.analysis.scopes :refer [analyze-scopes]]
            [quark.cogs.editor.analysis.symbols :refer [analyze-symbols]]
            [quark.cogs.editor.analysis.editing :refer [analyze-editing]]
            [quark.cogs.editor.analysis.defs :refer [analyze-defs]]
            [quark.cogs.editor.layout.soup :refer [build-soup-render-info]]
            [quark.cogs.editor.layout.code :refer [build-code-render-info]]
            [quark.cogs.editor.layout.docs :refer [build-docs-render-info]]
            [quark.cogs.editor.layout.headers :refer [build-headers-render-info]]
            [quark.cogs.editor.layout.selections :refer [build-selections-render-info]]
            [quark.cogs.editor.analyzer :refer [analyze-full]]
            [quark.cogs.editor.utils :refer [apply-to-selected-editors debug-print-analysis ancestor-count loc->path leaf-nodes make-zipper collect-all-right collect-all-parents collect-all-children valid-loc?]]
            [rewrite-clj.node :as node]
            [quark.util.helpers :as helpers]
            [clojure.zip :as z])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]]))

(defn extract-tokens [node]
  (if (= (:tag node) :token)
    [node]
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

(defn build-selectable-parents [loc selectables]
  (let [selectable? (fn [loc] (get selectables (:id (zip/node loc))))
        selectable-locs (filter selectable? (take-while valid-loc? (iterate z/next loc)))
        emit-item (fn [loc] [(:id (zip/node loc)) (first (rest (map #(:id (zip/node %)) (filter selectable? (collect-all-parents loc)))))])]
    (apply hash-map (mapcat emit-item selectable-locs))))

(defn prepare-form-render-info [editor loc]
  {:pre [(= (node/tag (zip/node (zip/up loc))) :forms)]}    ; parent has to be :forms
  (let [root-node (zip/node loc)
        _ (assert root-node)
        root-id (:id root-node)
        editing (or (get-in editor [:editing]) #{})
        nodes (apply hash-map (collect-nodes root-node))
        analysis (->> {}
                   (analyze-scopes root-node)
                   (analyze-symbols nodes)
                   (analyze-defs root-node)
                   (analyze-editing editing nodes))
        code-info (build-code-render-info analysis loc)
        docs-info (build-docs-render-info analysis loc)
        headers-info (build-headers-render-info analysis loc)
        all-tokens (extract-tokens code-info)
        all-token-ids (map :id all-tokens)
        tokens (zipmap all-token-ids all-tokens)
        selectables (apply hash-map (concat (extract-selectables-from-code code-info) (extract-selectables-from-docs docs-info)))
        lines-selectables (group-by :line (sort-by :id (filter #(= (:tag %) :token) (vals selectables))))
        selectable-parents (build-selectable-parents loc selectables)
        form-info {:id                 root-id
                   :editing            (not (empty? editing))
                   :nodes              nodes
                   :analysis           analysis
                   :tokens             tokens               ; used for soup generation
                   :selectables        selectables          ; used for selections
                   :lines-selectables  lines-selectables    ; used for left/right, up/down movement
                   :selectable-parents selectable-parents   ; used for level-up movement
                   :skelet             {:text    (zip/string loc) ; used for plain text debug view
                                        :code    code-info
                                        :docs    docs-info
                                        :headers headers-info}}]
    ;(debug-print-analysis root-node nodes analysis)
    (log "form" root-id "=> render-info:" form-info)
    form-info))

(defn prepare-render-infos-of-top-level-forms [editor form-id]
  (let [tree (:parse-tree editor)
        top (make-zipper tree)                              ; root "forms" node
        loc (zip/down top)                                  ; TODO: here we should skip possible whitespace
        right-siblinks (collect-all-right loc)              ; TODO: here we should use explicit zipping policy
        old-form-render-infos (get-in editor [:render-state :forms])
        find-old-form-render-info (fn [{:keys [id]}] (some #(if (= (:id %) id) %) old-form-render-infos))
        prepare-item #(merge (find-old-form-render-info (z/node %)) (prepare-form-render-info editor %))]
    (if form-id
      (map #(if (= (:id (z/node %)) form-id) (prepare-item %) (find-old-form-render-info (z/node %))) right-siblinks)
      (map prepare-item right-siblinks))))

(defn analyze-with-delay [editor-id delay]
  (go
    (<! (timeout delay))                                    ; give it some time to render UI (next requestAnimationFrame)
    (dispatch :editor-analyze editor-id)))

(defn layout-editor [form-id editor]
  (let [render-infos (prepare-render-infos-of-top-level-forms editor form-id)
        state {:forms            render-infos
               :debug-parse-tree (:parse-tree editor)}]
    (dispatch :editor-update-render-state (:id editor) state)
    #_(analyze-with-delay editor-id 1000))
  editor)

(defn update-layout [editors [editor-id form-id]]
  (apply-to-selected-editors (partial layout-editor form-id) editors editor-id))

(defn update-editor-layout-for-focused-form [editor]
  (let [focused-form-id (get-in editor [:selections :focused-form-id])]
    (layout-editor focused-form-id editor)))

(defn update-layout-for-focused-form [editors [editor-id]]
  (apply-to-selected-editors update-editor-layout-for-focused-form editors editor-id))

(defn update-render-state [editors [editor-id state]]
  (assoc-in editors [editor-id :render-state] state))

(defn update-form-soup-geometry [form geometry]
  (assoc form :soup (build-soup-render-info (:tokens form) geometry)))

(defn update-soup-geometry [editors [editor-id form-id geometry]]
  (let [forms (get-in editors [editor-id :render-state :forms])
        new-forms (map #(if (= (:id %) form-id) (update-form-soup-geometry % geometry) %) forms)
        new-editors (assoc-in editors [editor-id :render-state :forms] new-forms)]
    new-editors))

(defn update-form-selectables-geometry [form geometry]
  (assoc form :all-selections (build-selections-render-info (:selectables form) geometry)))

(defonce debounced-selection-updaters ^:dynamic {})

(defn dispatch-dispatch-editor-update-selections [editor-id]
  (dispatch :editor-update-selections editor-id))

(defn debounced-dispatch-editor-update-selections [editor-id]
  (if-let [updater (get debounced-selection-updaters editor-id)]
    (updater)
    (let [new-updater (helpers/debounce (partial dispatch-dispatch-editor-update-selections editor-id) 30)]
      (set! debounced-selection-updaters (assoc debounced-selection-updaters editor-id new-updater))
      (new-updater))))

(defn update-selectables-geometry [editors [editor-id form-id geometry]]
  (let [forms (get-in editors [editor-id :render-state :forms])
        new-forms (map #(if (= (:id %) form-id) (update-form-selectables-geometry % geometry) %) forms)
        new-editors (assoc-in editors [editor-id :render-state :forms] new-forms)]
    (debounced-dispatch-editor-update-selections editor-id) ; coalesce update-requests here
    new-editors))

(defn update-form-selections [form selected-node-ids]
  {:pre [(or (nil? selected-node-ids) (set? selected-node-ids))]}
  (let [all-selections (:all-selections form)
        matching-selections (filter #(contains? selected-node-ids (first %)) all-selections)]
    (assoc form :active-selections (vec matching-selections))))

(defn update-form-focus-flag [form focused-form-id]
  (assoc form :focused (= (:id form) focused-form-id)))

(defn update-selections [editors [editor-id]]
  (let [forms (get-in editors [editor-id :render-state :forms])
        selections (or (get-in editors [editor-id :selections]) {})
        get-form-selected-node-ids (fn [form] (get selections (:id form)))
        process-form (fn [form] (-> form
                                  (update-form-selections (get-form-selected-node-ids form))
                                  (update-form-focus-flag (:focused-form-id selections))))
        new-forms (map process-form forms)
        new-editors (assoc-in editors [editor-id :render-state :forms] new-forms)]
    new-editors))

(defn analyze [editors [editor-id]]
  (let [ast (analyze-full (get-in editors [editor-id :text]) {:atom-path (get-in editors [editor-id :def :uri])})]
    (log "AST>" ast))
  editors)

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-update-layout paths/editors-path update-layout)
(register-handler :editor-update-layout-for-focused-form paths/editors-path update-layout-for-focused-form)
(register-handler :editor-update-render-state paths/editors-path update-render-state)
(register-handler :editor-update-soup-geometry paths/editors-path update-soup-geometry)
(register-handler :editor-update-selectables-geometry paths/editors-path update-selectables-geometry)
(register-handler :editor-update-selections paths/editors-path update-selections)
(register-handler :editor-analyze paths/editors-path analyze)
