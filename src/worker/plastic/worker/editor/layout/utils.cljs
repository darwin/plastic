(ns plastic.worker.editor.layout.utils
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.common :refer [process]])
  (:require [clojure.string :as string]
            [meld.zip :as zip]
            [meld.node :as node]))

; -------------------------------------------------------------------------------------------------------------------

(defn unwrap-metas [nodes]
  nodes
  #_(let [unwrap-meta-node (fn [node]
                           (if (= (node/tag node) :meta)
                             (unwrap-metas (node/children node))
                             [node]))]
    (mapcat unwrap-meta-node nodes)))

;(defn first-word [s]
;  (first (string/split s #"\s")))

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

(defn first-child-source [loc]
  (if-let [first-child-loc (zip/down loc)]
    (zip/get-source first-child-loc)))

(defn is-def? [loc]
  (if (zip/list? loc)
    (re-find #"^def" (first-child-source loc))))

(defn is-defn? [loc]
  (if (zip/list? loc)
    (re-find #"^defn" (first-child-source loc))))

(defn doc? [loc]
  (if (zip/string? loc)
    (if-let [parent-loc (zip/up loc)]
      (if (is-defn? parent-loc)
        (let [left-strings (filter node/string? (zip/lefts loc))]
          (empty? left-strings))))))

(defn def-name? [loc]
  (if (zip/symbol? loc)
    (if-let [parent-loc (zip/up loc)]
      (if (is-def? parent-loc)
        (let [left-symbols (filter node/symbol? (zip/lefts loc))]
          (= 1 (count left-symbols)))))))

(defn linebreak-near-doc? [loc dir]
  (if (zip/linebreak? loc)
    (let [test-loc (dir loc)]
      (if (zip/good? test-loc)
        (doc? test-loc)))))

(defn standalone-comment? [loc]
  (if (zip/comment? loc)
    (let [prev-loc (zip/prev loc)
          next-loc (zip/next loc)
          prev-linebreak? (or (not (zip/good? prev-loc)) (zip/linebreak? prev-loc))
          next-linebreak? (or (not (zip/good? next-loc)) (zip/linebreak? next-loc))]
      (and prev-linebreak? next-linebreak?))))

(defn linebreak-near-comment? [loc dir]
  (if (zip/linebreak? loc)
    (let [test-loc (dir loc)]
      (if (zip/good? test-loc)
        (zip/comment? test-loc)))))

(defn lookup-arities [loc]
  (let [first-vec (first (filter zip/vector? (take-while zip/good? (iterate zip/right loc))))]
    (if (zip/good? first-vec)
      [(zip/get-source first-vec)]
      (let [lists (filter zip/list? (take-while zip/good? (iterate zip/right loc)))]
        (if (seq lists)
          (vec (map #(zip/get-source (zip/down %)) lists)))))))

(defn spatial? [layout-info]
  (#{:token :spot :doc :linebreak :comment} (:type layout-info)))
