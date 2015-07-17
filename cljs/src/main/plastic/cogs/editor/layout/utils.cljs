(ns plastic.cogs.editor.layout.utils
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.node :as node]
            [clojure.string :as string]
            [rewrite-clj.node.stringz :refer [StringNode]]
            [rewrite-clj.node.token :refer [TokenNode]]
            [rewrite-clj.node.keyword :refer [KeywordNode]]
            [rewrite-clj.zip :as zip]
            [clojure.zip :as z]
            [plastic.util.zip :as zip-utils]))

(defn unwrap-metas [nodes]
  (let [unwrap-meta-node (fn [node]
                           (if (= (node/tag node) :meta)
                             (unwrap-metas (node/children node))
                             [node]))]
    (mapcat unwrap-meta-node nodes)))

(defn first-word [s]
  (first (string/split s #"\s")))

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

(defn replace-unescape-double-quotes [s]
  (string/replace s #"\\\"" "\""))

(defn prepare-string-for-display [s]
  (-> s
    strip-indent
    strip-double-quotes
    replace-unescape-double-quotes))

(defn wrap-in-span
  ([text class] (wrap-in-span true text class))
  ([pred text class] (if pred (str "<span class=\"" class "\">" text "</span>") text)))

(defn is-selectable? [tag]
  (#{:token :fn :list :map :vector :set :meta} tag))

(defn selector-matches-editor? [editor-id selector]
  (cond
    (vector? selector) (some #{editor-id} selector)
    (set? selector) (contains? selector editor-id)
    :default (= editor-id selector)))

(defn apply-to-specified-editors [f editors id-or-ids]
  (apply array-map
    (flatten
      (for [[editor-id editor] editors]
        (if (selector-matches-editor? editor-id id-or-ids)
          [editor-id (f editor)]
          [editor-id editor])))))

(defn doall-specified-editors [f editors id-or-ids]
  (doall
    (for [[editor-id editor] editors]
      (if (selector-matches-editor? editor-id id-or-ids)
        (f editor)))))

(defn string-node? [node]
  (instance? StringNode node))

(defn symbol-node? [node]
  (instance? TokenNode node))

(defn keyword-node? [node]
  (instance? KeywordNode node))

(defn string-loc? [loc]
  (string-node? (z/node loc)))

(defn symbol-loc? [loc]
  (symbol-node? (z/node loc)))

(defn first-child-sexpr [loc]
  (first (node/child-sexprs (zip/node loc))))

(defn is-def? [loc]
  (if (= (zip/tag loc) :list)
    (re-find #"^def" (str (first-child-sexpr loc)))))

(defn is-doc? [loc]
  (if (string-loc? loc)
    (if-let [parent-loc (zip/up loc)]
      (if (is-def? parent-loc)
        (let [left-strings (filter string-node? (z/lefts loc))]
          (empty? left-strings))))))

(defn is-def-name? [loc]
  (if (symbol-loc? loc)
    (if-let [parent-loc (zip/up loc)]
      (if (is-def? parent-loc)
        (let [left-symbols (filter symbol-node? (z/lefts loc))]
          (= 1 (count left-symbols)))))))

(defn is-whitespace-or-nl-after-doc? [loc]
  (if (node/whitespace? (zip/node loc))
    (let [left-loc (zip/left loc)]
      (if (zip-utils/valid-loc? left-loc)
        (is-doc? left-loc)))))