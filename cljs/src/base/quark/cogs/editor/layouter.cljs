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
            [quark.cogs.editor.utils :refer [node-interesting? leaf-nodes ancestor-count make-rpath make-zipper collect-all-right]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]
                   [cljs.core.async.macros :refer [go]]))

(defn item-info [loc]
  (let [node (zip/node loc)]
    (if (node/comment? node)
      {:tag   :newline
       :depth (ancestor-count loc)
       :path  (make-rpath loc)
       :text  ("\n")}
      {:string (zip/string loc)
       :tag    (node/tag node)
       :depth  (ancestor-count loc)
       :path   (make-rpath loc)})))

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
        (log indent-match indent)
        (string/join "\n"(map #(string/replace % re "") lines)))
      s)))

(defn replace-whitespace-characters [s]
  (-> s
    (string/replace #"\n" "↵\n")
    (string/replace #" " "␣")
    (string/replace #"\t" "⇥")))

(defn prepare-string-for-display [s]
  (-> s strip-indent strip-double-quotes replace-whitespace-characters))

(defn build-structure [depth node]
  (merge {:tag   (node/tag node)
          :depth depth}
    (if (node/inner? node)
      {:children (doall (map (partial build-structure (inc depth)) (interesting-children node)))}
      {:text (node/string node)})
    (if (node/comment? node)
      {:tag :newline
       :text "\n"})
    (if (instance? StringNode node)
      {:text (prepare-string-for-display (node/string node))
       :tag "string"})
    (if (instance? KeywordNode node)
      {:tag "keyword"})))

(defn form-info [loc]
  (let [node (zip/node loc)]
    {:text      (zip/string loc)
     :soup      (build-soup node)
     :structure (build-structure 0 node)}))

(defn extract-top-level-form-infos [node]
  (let [loc (make-zipper node)
        right-siblinks (collect-all-right loc)]
    (doall (map form-info right-siblinks))))

(defn analyze-with-delay [editor-id delay]
  (go
    (<! (timeout delay))                                    ; give it some time to render UI (next requestAnimationFrame)
    (dispatch :editor-analyze editor-id)))

(defn layout [editors [editor-id]]
  (let [parse-tree (get-in editors [editor-id :parse-tree])
        top-level-forms (extract-top-level-form-infos parse-tree)
        state {:forms      top-level-forms
               :parse-tree parse-tree}]
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