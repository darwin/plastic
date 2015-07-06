(ns quark.cogs.editor.layouter
  (:require [cljs.core.async :refer [<! timeout]]
            [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [quark.util.helpers :as helpers]
            [quark.cogs.editor.analysis.scopes :refer [analyze-scopes]]
            [quark.cogs.editor.analysis.symbols :refer [analyze-symbols]]
            [quark.cogs.editor.analysis.defs :refer [analyze-defs]]
            [quark.cogs.editor.analysis.cursors :refer [analyze-cursors]]
            [quark.dev.formatting :refer [devtools-formatting!]]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [clojure.string :as string]
            [rewrite-clj.node.stringz :refer [StringNode]]
            [rewrite-clj.node.keyword :refer [KeywordNode]]
            [quark.cogs.editor.analyzer :refer [analyze-full]]
            [quark.cogs.editor.utils :refer [layouting-children-zip layouting-children essential-children node-walker node-interesting? leaf-nodes ancestor-count loc->path make-zipper collect-all-right]]
            [clojure.zip :as z])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]]))

(defn item-info [loc]
  (let [node (zip/node loc)]
    (if (node/comment? node)
      {:tag   :newline
       :depth (ancestor-count loc)
       :path  (loc->path loc)
       :text  ("\n")}
      {:string (zip/string loc)
       :tag    (node/tag node)
       :depth  (ancestor-count loc)
       :path   (loc->path loc)})))

(defn build-soup [node]
  (map item-info (leaf-nodes (make-zipper node))))

(defn strip-double-quotes [s]
  (-> s
    (string/replace #"^\"" "")
    (string/replace #"\"$" "")))

; TODO: this should be more robust by reading previous whitespace Node before string node
; here we do indent heuristics from string itself by looking at second line
; and assuming it doesn't have extra indent against first line
(defn strip-indent [s]
  (let [lines (string/split-lines s)
        second-line (second lines)]
    (if second-line
      (let [indent-match (.match second-line #"^[ ]*")
            indent (first indent-match)
            re (js/RegExp. (str "^" indent) "g")]
        (string/join "\n" (map #(string/replace % re "") lines)))
      s)))

(defn replace-whitespace-characters [s]
  (-> s
    (string/replace #"\n" "↵\n")
    (string/replace #" " "␣")
    (string/replace #"\t" "⇥")))

(defn prepare-string-for-display [s]
  (-> s
    strip-indent
    strip-double-quotes
    replace-whitespace-characters))

(defn is-whitespace-or-nl-after-def-doc? [analysis loc]
  (let [node (zip/node loc)]
    (if (or (node/whitespace? node) (node/linebreak? node))
      (let [prev (z/left loc)
            prev-node (zip/node prev)
            prev-node-analysis (get analysis prev-node)]
        (:def-doc? prev-node-analysis)))))

(defn build-node-code-tree [depth scope-id analysis loc]
  (let [node (zip/node loc)
        node-analysis (get analysis node)
        new-scope-id (get-in node-analysis [:scope :id])
        {:keys [declaration-scope def-name? def-doc? cursor]} node-analysis
        {:keys [shadows decl?]} declaration-scope]

    (if (or def-doc? (is-whitespace-or-nl-after-def-doc? analysis loc))
      nil
      (merge
        {:tag   (node/tag node)
         :depth depth}
        (if (node/inner? node)
          {:children (remove nil? (map (partial build-node-code-tree (inc depth) new-scope-id analysis) (layouting-children-zip loc)))}
          {:text (node/string node)})
        (if (node/comment? node)
          {:tag  :newline
           :text "\n"})
        (if (instance? StringNode node)
          {:text (prepare-string-for-display (node/string node))
           :tag  :string})
        (if (instance? KeywordNode node)
          {:tag :keyword})
        (if (not= new-scope-id scope-id)
          {:scope new-scope-id})
        (if def-name?
          {:def-name? true})
        (if declaration-scope
          {:decl-scope (:id declaration-scope)})
        (if cursor
          {:cursor true})
        (if shadows
          {:shadows shadows})
        (if decl?
          {:decl? decl?})))))

(defn build-code-tree [analysis node]
  (build-node-code-tree 0 nil analysis node))

(defn doc-item [[_node info]]
  (let [name-node (:def-name-node info)
        name (if name-node (node/string name-node))
        doc-node (:def-doc-node info)
        doc (if doc-node (node/string doc-node))]
  (merge
    (if name {:name name})
    (if doc {:doc (prepare-string-for-display doc)}))))

(defn build-docs-tree [analysis _node]
  (let [docs (filter (fn [[_node info]] (:def? info)) analysis)]
    (map doc-item docs)))

(defn form-layout-info [editor loc]
  (let [node (zip/node loc)
        analysis (->> {}
                   (analyze-scopes node)
                   (analyze-symbols node)
                   (analyze-defs node)
                   (analyze-cursors editor))]
    (log "ANALYSIS:" analysis)
    {:text      (zip/string loc)
     :soup      (build-soup node)
     :code-tree (build-code-tree analysis loc)
     :docs-tree (build-docs-tree analysis loc)}))

(defn prepare-top-level-forms [editor]
  (let [node (get editor :parse-tree)
        top (make-zipper node) ; root "forms" node
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