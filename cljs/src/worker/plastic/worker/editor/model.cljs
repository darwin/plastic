(ns plastic.worker.editor.model
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.findz :as findz]
            [rewrite-clj.node :as node]
            [plastic.worker.editor.parser.utils :as parser]
            [plastic.util.helpers :as helpers]
            [plastic.worker.editor.layout.builder :as layout]
            [plastic.util.zip :as zip-utils]
            [rewrite-clj.node.whitespace :refer [whitespace-node]]
            [rewrite-clj.node.token :refer [token-node]]
            [rewrite-clj.node.keyword :refer [keyword-node]]
            [rewrite-clj.node.whitespace :refer [newline-node]]
            [rewrite-clj.node.meta :refer [meta-node]]
            [rewrite-clj.node.fn :refer [fn-node]]
            [rewrite-clj.node.seq :refer [list-node vector-node map-node set-node]]
            [rewrite-clj.node.reader-macro :refer [deref-node]]
            [rewrite-clj.node.quote :refer [quote-node]]
            [reagent.ratom :refer [IDisposable dispose!]]
            [clojure.zip :as z]
            [plastic.worker.editor.toolkit.id :as id]))

; a kitchen-sink with helpers for manipulating editor data structure
; also see 'xforms' folder for more high-level editor transformations

(defprotocol IEditor)

(defrecord Editor [id def]
  IEditor)

(defn make [editor-id editor-def]
  (Editor. editor-id editor-def))

(defn valid-editor? [editor]
  (satisfies? IEditor editor))

(defn valid-editor-id? [editor-id]
  (pos? editor-id))

; -------------------------------------------------------------------------------------------------------------------

(defn get-id [editor]
  {:post [(pos? %)]}
  (:id editor))

; -------------------------------------------------------------------------------------------------------------------

(defn register-reaction [editor reaction]
  (update editor :reactions (fn [reactions] (conj (or reactions []) reaction))))

(defn dispose-reactions! [editor]
  (doseq [reaction (:reactions editor)]
    (dispose! reaction))
  (dissoc editor :reactions))

; -------------------------------------------------------------------------------------------------------------------

; note comments are treated as line-breaks because they have new-lines embedded
(defn strip-whitespaces-but-keep-linebreaks-policy [loc]
  (let [node (z/node loc)]
    (and (or (node/linebreak? node) (not (node/whitespace? node))))))

(def zip-down (partial zip-utils/zip-down strip-whitespaces-but-keep-linebreaks-policy))
(def zip-right (partial zip-utils/zip-right strip-whitespaces-but-keep-linebreaks-policy))
(def zip-left (partial zip-utils/zip-left strip-whitespaces-but-keep-linebreaks-policy))

; -------------------------------------------------------------------------------------------------------------------

(defn get-parse-tree [editor]
  {:pre [(valid-editor? editor)]}
  (let [parse-tree (get editor :parse-tree)]
    (assert parse-tree)
    parse-tree))

(defn set-parse-tree [editor parse-tree]
  {:pre [(valid-editor? editor)
         (= (node/tag parse-tree) :forms)]}
  (assoc editor :parse-tree parse-tree))

(defn parsed? [editor]
  {:pre [(valid-editor? editor)]}
  (contains? editor :parse-tree))

; -------------------------------------------------------------------------------------------------------------------

(defn get-render-state [editor]
  {:pre [(valid-editor? editor)]}
  (get editor :render-state))

(defn set-render-state [editor render-state]
  {:pre [(valid-editor? editor)]}
  (let [old-render-state (get-render-state editor)]
    (if (not= old-render-state render-state)
      (assoc-in editor [:render-state] render-state)
      editor)))

; -------------------------------------------------------------------------------------------------------------------

(defn set-editing [editor node-id]
  {:pre [(valid-editor? editor)]}
  (if node-id
    (assoc editor :editing #{node-id})
    (assoc editor :editing #{})))

(defn editing? [editor]
  {:pre [(valid-editor? editor)]}
  (let [editing (:editing editor)]
    (and editing (not (empty? editing)))))

; -------------------------------------------------------------------------------------------------------------------

(defn transform-parse-tree [editor transformation]
  {:pre [(valid-editor? editor)]}
  (let [new-parse-tree (transformation (get-parse-tree editor))]
    (set-parse-tree editor new-parse-tree)))

(defn get-top-level-locs [editor]
  {:pre [(valid-editor? editor)]}
  (let [top-loc (zip-utils/make-zipper (get-parse-tree editor))                                                       ; root "forms" node
        first-top-level-form-loc (zip/down top-loc)]
    (zip-utils/collect-all-right first-top-level-form-loc)))                                                          ; TODO: here we should use explicit zipping policy

(defn find-node-loc [editor node-id]
  {:pre [(valid-editor? editor)]}
  (let [parse-tree (get-parse-tree editor)
        root-loc (zip-utils/make-zipper parse-tree)
        node-loc (findz/find-depth-first root-loc (partial zip-utils/loc-id? node-id))]
    node-loc))

(defn find-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (let [node-loc (find-node-loc editor node-id)]
    (assert (zip-utils/valid-loc? node-loc))
    (zip/node node-loc)))

(defn transfer-sticky-attrs [old new]
  (let [sticky-bits (select-keys old [:id])]
    (merge new sticky-bits)))

(defn empty-value? [node]
  (let [text (node/string node)
        sexpr (node/sexpr node)]
    (or
      (and (symbol? sexpr) (empty? text))
      (and (keyword? sexpr) (= ":" text)))))

(defn commit-value-to-loc [loc node-id new-node]
  {:pre [(:id new-node)]}
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))]
    (assert (zip-utils/valid-loc? node-loc))
    (if-not (empty-value? new-node)
      (z/edit node-loc (fn [old-node] (transfer-sticky-attrs old-node new-node)))
      (z/remove node-loc))))

(defn delete-whitespaces-and-newlines-after-loc [loc]
  (loop [cur-loc loc]
    (let [possibly-white-space-loc (z/right cur-loc)]
      (if (and
            (zip-utils/valid-loc? possibly-white-space-loc)
            (node/whitespace? (zip/node possibly-white-space-loc)))
        (recur (z/remove possibly-white-space-loc))
        cur-loc))))

(defn delete-whitespaces-and-newlines-before-loc [loc]
  (loop [cur-loc loc]
    (let [possibly-white-space-loc (z/left cur-loc)]
      (if (and
            (zip-utils/valid-loc? possibly-white-space-loc)
            (node/whitespace? (zip/node possibly-white-space-loc)))
        (let [loc-after-removal (z/remove possibly-white-space-loc)
              next-step-loc (z/next loc-after-removal)]
          (recur next-step-loc))
        cur-loc))))

(defn delete-node-loc [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))]
    (assert (zip-utils/valid-loc? node-loc))
    (z/remove (delete-whitespaces-and-newlines-before-loc node-loc))))

(defn parse-tree-transformer [f]
  (fn [parse-tree]
    (if-let [result (f (zip-utils/make-zipper parse-tree))]
      (z/root result)
      parse-tree)))

(defn delete-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor (parse-tree-transformer (partial delete-node-loc (id/id-part node-id)))))

(defn commit-node-value [editor node-id value]
  {:pre [(valid-editor? editor)
         node-id
         value]}
  (let [old-root (get-parse-tree editor)
        root-loc (zip-utils/make-zipper old-root)
        modified-loc (commit-value-to-loc root-loc node-id value)]
    (if-not modified-loc
      editor
      (set-parse-tree editor (zip/root modified-loc)))))

(defn insert-values-after-node-loc [node-id values loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        inserter (fn [loc val] {:pre [(:id val)]} (z/insert-right loc val))]
    (reduce inserter node-loc (reverse values))))

(defn insert-values-before-node-loc [node-id values loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        inserter (fn [loc val] {:pre [(:id val)]} (z/insert-left loc val))]
    (reduce inserter node-loc values)))

(defn insert-values-before-first-child-of-node-loc [node-id values loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        first-child-loc (zip-down node-loc)
        _ (assert (zip-utils/valid-loc? first-child-loc))
        inserter (fn [loc val] {:pre [(:id val)]} (z/insert-left loc val))]
    (reduce inserter first-child-loc values)))

(defn insert-values-after-node [editor node-id values]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial insert-values-after-node-loc (id/id-part node-id) values))))

(defn insert-values-before-node [editor node-id values]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial insert-values-before-node-loc (id/id-part node-id) values))))

(defn insert-values-before-first-child-of-node [editor node-id values]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial insert-values-before-first-child-of-node-loc (id/id-part node-id) values))))

(defn remove-linebreak-before-node-loc [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        prev-locs (take-while zip-utils/valid-loc? (iterate z/prev node-loc))
        first-linebreak (first (filter #(node/linebreak? (zip/node %)) prev-locs))]
    (if first-linebreak
      (z/remove first-linebreak))))

(defn remove-linebreak-before-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial remove-linebreak-before-node-loc (id/id-part node-id)))))

(defn remove-right-siblink-of-loc [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        right-loc (zip-right node-loc)]
    (if right-loc
      (loop [loc right-loc]
        (let [new-loc (z/remove loc)]
          (if (= (zip-utils/loc-id new-loc) node-id)
            new-loc
            (recur new-loc)))))))

(defn remove-right-siblink [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial remove-right-siblink-of-loc (id/id-part node-id)))))

(defn remove-left-siblink-of-loc [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        left-loc (zip-left node-loc)]
    (if left-loc
      (z/remove left-loc))))

(defn remove-left-siblink [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial remove-left-siblink-of-loc (id/id-part node-id)))))

(defn remove-first-child-of-node-loc [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial zip-utils/loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        first-child-loc (zip-down node-loc)]
    (if first-child-loc
      (loop [loc first-child-loc]
        (let [new-loc (z/remove loc)]
          (if (= (zip-utils/loc-id new-loc) node-id)
            new-loc
            (recur new-loc)))))))

(defn remove-first-child-of-node [editor node-id]
  {:pre [(valid-editor? editor)]}
  (transform-parse-tree editor
    (parse-tree-transformer (partial remove-first-child-of-node-loc (id/id-part node-id)))))

; -------------------------------------------------------------------------------------------------------------------

(defn prepare-placeholder-node []
  (parser/assoc-node-id (token-node "" "")))

(defn prepare-newline-node []
  (parser/assoc-node-id (newline-node "\n")))

(defn prepare-keyword-node [k]
  (parser/assoc-node-id (keyword-node k)))

(defn prepare-list-node [children]
  (parser/assoc-node-id (list-node children)))

(defn prepare-vector-node [children]
  (parser/assoc-node-id (vector-node children)))

(defn prepare-map-node [children]
  (parser/assoc-node-id (map-node children)))

(defn prepare-set-node [children]
  (parser/assoc-node-id (set-node children)))

(defn prepare-fn-node [children]
  (parser/assoc-node-id (fn-node children)))

(defn prepare-meta-node [children]
  (parser/assoc-node-id (meta-node children)))

(defn prepare-quote-node [children]
  (parser/assoc-node-id (quote-node children)))

(defn prepare-deref-node [children]
  (parser/assoc-node-id (deref-node children)))

; -------------------------------------------------------------------------------------------------------------------

(defn selector-matches-editor? [editor-id selector]
  {:pre [(valid-editor-id? editor-id)]}
  (cond
    (vector? selector) (some #{editor-id} selector)
    (set? selector) (contains? selector editor-id)
    :default (= editor-id selector)))

(defn apply-to-editors [editors selector f]
  (let [reducer (fn [editors editor-id]
                  (or
                    (if (selector-matches-editor? editor-id selector)
                      (if-let [new-editor (f (get editors editor-id))]
                        (assoc editors editor-id new-editor)))
                    editors))]
    (reduce reducer editors (keys editors))))

(defn apply-to-forms [editor selector f]
  {:pre [(valid-editor? editor)]}
  (let [reducer (fn [editor form-loc]
                  (if (helpers/selector-matches? selector (zip-utils/loc-id form-loc))
                    (or (f editor form-loc) editor)
                    editor))]
    (reduce reducer editor (get-top-level-locs editor))))

; -------------------------------------------------------------------------------------------------------------------

(defn get-previously-layouted-form-node [editor form-id]
  {:pre [(valid-editor? editor)]}
  (get-in editor [:previously-layouted-forms form-id]))

(defn remember-previously-layouted-form-node [editor form-node]
  {:pre [(valid-editor? editor)]}
  (assoc-in editor [:previously-layouted-forms (:id form-node)] form-node))

(defn prune-cache-of-previously-layouted-forms [editor form-ids-to-keep]
  {:pre [(valid-editor? editor)]}
  (let [form-nodes (get editor :previously-layouted-forms)]
    (assoc editor :previously-layouted-forms (select-keys form-nodes form-ids-to-keep))))