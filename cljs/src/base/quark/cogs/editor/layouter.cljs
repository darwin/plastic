(ns quark.cogs.editor.layouter
  (:require [cljs.core.async :refer [<! timeout]]
            [quark.frame.core :refer [subscribe register-handler]]
            [quark.schema.paths :as paths]
            [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [clojure.string :as string]
            [rewrite-clj.node.stringz :refer [StringNode]]
            [rewrite-clj.node.keyword :refer [KeywordNode]]
            [quark.cogs.editor.analyzer :refer [analyze-full]]
            [quark.cogs.editor.utils :refer [node-interesting? leaf-nodes ancestor-count make-path make-zipper collect-all-right]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]]))

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

(declare build-structure)

(defn interesting-children [node]
  (filter node-interesting? (node/children node)))

(defn strip-double-quotes [s]
  (-> s
    (string/replace #"^\"" "")
    (string/replace #"\"$" "")))

; TODO: this should be more robust by reading previous whitespace Node before string note
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
  (-> s strip-indent strip-double-quotes replace-whitespace-characters))

(defn build-structure [depth scope-id lexical-info node]
  (let [lexical (get lexical-info node)
        new-scope-id (get-in lexical [:scope :id])
        decl-scope (get-in lexical [:declaration-scope])]
    (merge {:tag   (node/tag node)
            :depth depth}
      (if (node/inner? node)
        {:children (doall (map (partial build-structure (inc depth) new-scope-id lexical-info) (interesting-children node)))}
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
        {:scope (get-in lexical [:scope :id])})
      (if decl-scope
        {:decl-scope (:id decl-scope)
         :shadows    (:shadows decl-scope)}))))

(declare analyze-lexical-info)

(defonce ^:dynamic scope-id 0)

(defn next-scope-id! []
  (set! scope-id (inc scope-id))
  scope-id)

(def lexical-openers
  {'defn :params
   'fn   :params
   'let  :pairs})

(defn filter-non-args [arg-nodes]
  (let [arg? (fn [[node _]]
               (let [s (node/string node)]
                 (not (or (= s "&") (= (first s) "_")))))]
    (filter arg? arg-nodes)))

(defn get-max-id [node current]
  (if (node/inner? node)
    (max current (:id node) (apply max (map :id (node/children node))))
    (max current (:id node))))

; TODO: here must be proper parsing of destructuring
(defn collect-vector-params [node]
  (filter-non-args (map (fn [node] [node (:id node)]) (filter (complement node/whitespace?) (node/children node)))))

; TODO: here must be proper parsing of destructuring
(defn collect-vector-pairs [node]
  (filter-non-args
    (let [pairs (partition 2 (filter (complement node/whitespace?) (node/children node)))]
      (for [pair pairs]
        [(first pair) (get-max-id (second pair) 0)]))))

(defn collect-params [node opener-type]
  (if-let [first-vector (first (filter #(= (node/tag %) :vector) (node/children node)))]
    (condp = opener-type
      :params (collect-vector-params first-vector)
      :pairs (collect-vector-pairs first-vector))))

(defn node-lexical-info [node]
  (if (= (node/tag node) :list)
    (if-let [opener-type (lexical-openers (node/sexpr (first (node/children node))))]
      {:id     (next-scope-id!)
       :locals (collect-params node opener-type)})))

(defn child-scopes [scope node]
  (if (node/inner? node)
    (let [res (map (partial analyze-lexical-info scope) (interesting-children node))]
      (apply merge res))
    {}))

(defn analyze-lexical-info [scope-info node]
  (if-let [new-scope (node-lexical-info node)]
    (let [new-scope-info {:scope new-scope :parent-scope scope-info}]
      (merge
        {node new-scope-info}
        (child-scopes new-scope-info node)))
    (merge
      {node scope-info}
      (child-scopes scope-info node))))

(defn mapf [m f]
  (into {} (for [[k v] m] [k (f k v)])))

(defn local-valid? [node [decl-node after-id]]
  (or
    (identical? node decl-node)
    (and
      (> (:id node) after-id)
      (= (node/string decl-node) (node/string node)))))

(defn find-declaration [node scope-info]
  (if scope-info
    (let [locals (get-in scope-info [:scope :locals])
          matching-locals (filter (partial local-valid? node) locals)]
      (if-not (empty? matching-locals)
        (assoc (:scope scope-info) :shadows (count matching-locals))
        (find-declaration node (:parent-scope scope-info))))))

(defn resolve-var [node scope-info]
  (if (= (node/tag node) :token)
    (if-let [declaration-scope (find-declaration node scope-info)]
      (assoc scope-info :declaration-scope declaration-scope)
      scope-info)
    scope-info))

(defn resolve-vars [lexical]
  (mapf lexical resolve-var))

(defn form-info [loc]
  (let [node (zip/node loc)
        lexical-info (binding [scope-id 0]
                       (analyze-lexical-info {:scope nil :parent-scope nil} node))
        resolved-vars (resolve-vars lexical-info)]
    ;(doall (map (fn [x] (if-not (node/printable-only? (first x)) (log (node/sexpr (first x)) (second x)))) resolved-vars))
    {:text      (zip/string loc)
     :soup      (build-soup node)
     :structure (build-structure 0 nil resolved-vars node)}))

(defn extract-top-level-form-infos [node]
  (let [loc (make-zipper node)
        right-siblinks (collect-all-right loc)]
    (doall (map form-info right-siblinks))))

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