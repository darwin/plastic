(ns quark.cogs.editor.layouter
  (:require [cljs.core.async :refer [<! timeout]]
            [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [quark.util.helpers :as helpers]
            [quark.cogs.editor.analysis.scopes :refer [analyze-scopes]]
            [quark.cogs.editor.analysis.symbols :refer [analyze-symbols]]
            [quark.cogs.editor.analysis.defs :refer [analyze-defs]]
            [quark.dev.formatting :refer [devtools-formatting!]]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [clojure.string :as string]
            [rewrite-clj.node.stringz :refer [StringNode]]
            [rewrite-clj.node.keyword :refer [KeywordNode]]
            [quark.cogs.editor.analyzer :refer [analyze-full]]
            [quark.cogs.editor.utils :refer [layouting-children essential-children node-walker node-interesting? leaf-nodes ancestor-count make-path make-zipper collect-all-right]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]]))

(devtools-formatting!)

(defn item-info [loc]
  (let [node (zip/node loc)]
    (if (node/comment? node)
      {:tag   :newline
       :depth (ancestor-count loc)
       :path  (make-path loc)
       :text  ("\n")}
      {:string (zip/string loc)
       :tag    (node/tag node)
       :depth  (ancestor-count loc)
       :path   (make-path loc)})))

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

(defn build-node-layout [depth scope-id analysis node]
  (let [node-analysis (get analysis node)
        new-scope-id (get-in node-analysis [:scope :id])
        decl-scope (get-in node-analysis [:declaration-scope])
        def-name? (get node-analysis :def-name?)
        shadows (:shadows decl-scope)
        decl? (:decl? decl-scope)]
    (merge {:tag   (node/tag node)
            :depth depth}
      (if (node/inner? node)
        {:children (doall (map (partial build-node-layout (inc depth) new-scope-id analysis) (layouting-children node)))}
        {:text (node/string node)})
      (if (node/comment? node)
        {:tag  :newline
         :text "\n"})
      (if (instance? StringNode node)
        {:text (prepare-string-for-display (node/string node))
         :tag  "string"})
      (if (instance? KeywordNode node)
        {:tag "keyword"})
      (if (not= new-scope-id scope-id)
        {:scope new-scope-id})
      (if def-name?
        {:def-name? true})
      (if decl-scope
        {:decl-scope (:id decl-scope)})
      (if shadows
        {:shadows shadows})
      (if decl?
        {:decl? decl?}))))

(defn build-layout [analysis node]
  (build-node-layout 0 nil analysis node))

(defn form-layout-info [loc]
  (let [node (zip/node loc)
        analysis (->> {}
                   (analyze-scopes node)
                   (analyze-symbols node)
                   (analyze-defs node))]
    (log analysis)
    {:text      (zip/string loc)
     :soup      (build-soup node)
     :structure (build-layout analysis node)}))

(defn extract-top-level-form-infos [node]
  (let [loc (make-zipper node)
        right-siblinks (collect-all-right loc)]
    (doall (map form-layout-info right-siblinks))))

(defn analyze-with-delay [editor-id delay]
  (go
    (<! (timeout delay))                                    ; give it some time to render UI (next requestAnimationFrame)
    (dispatch :editor-analyze editor-id)))

(defn layout-editor [editor editor-id]
  (let [parse-tree (get editor :parse-tree)
        top-level-forms (extract-top-level-form-infos parse-tree)
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