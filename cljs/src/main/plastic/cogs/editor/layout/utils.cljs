(ns plastic.cogs.editor.layout.utils
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.node :as node]
            [clojure.string :as string]))

(defn node-walker [inner-fn leaf-fn reducer child-selector]
  (let [walker (fn walk [node]
                 (if (node/inner? node)
                   (let [node-results (inner-fn node)
                         children-results (mapcat walk (child-selector node))
                         results (apply reducer (concat children-results node-results))]
                     [results])
                   (leaf-fn node)))]
    (fn [node]
      (apply reducer (walker node)))))

(defn unwrap-metas [nodes]
  (let [unwrap-meta-node (fn [node]
                           (if (= (node/tag node) :meta)
                             (unwrap-metas (node/children node))
                             [node]))]
    (mapcat unwrap-meta-node nodes)))

(defn first-word [s]
  (first (string/split s #"\s")))

(defn debug-print-analysis [node nodes analysis]
  (group "ANALYSIS of" (:id node) "\n" (node/string node))
  (doseq [[id info] (sort analysis)]
    (log id (first-word (node/string (get nodes id))) info))
  (group-end))

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

(defn apply-to-selected-editors [f editors id-or-ids]
  (apply array-map
    (flatten
      (for [[editor-id editor] editors]
        (if (selector-matches-editor? editor-id id-or-ids)
          [editor-id (f editor)]
          [editor-id editor])))))