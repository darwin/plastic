(ns plastic.cogs.editor.model
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.findz :as findz]
            [rewrite-clj.node :as node]
            [clojure.walk :refer [prewalk]]
            [plastic.cogs.editor.parser.utils :as parser]
            [plastic.util.helpers :as helpers]
            [plastic.util.zip :as zip-utils]
            [rewrite-clj.node.token :refer [token-node]]
            [rewrite-clj.node.whitespace :refer [newline-node]]
            [clojure.zip :as z]))

; a kitchen-sink with helpers for manipulating editor data structure
; also see 'ops' folder for more high-level editor transformations

(defn parsed? [editor]
  (contains? editor :parse-tree))

(defn get-parse-tree [editor]
  (let [parse-tree (get editor :parse-tree)]
    (assert parse-tree)
    parse-tree))

(defn set-parse-tree [editor parse-tree]
  {:pre [(= (node/tag parse-tree) :forms)]}
  (assoc editor :parse-tree parse-tree))

(defn get-selection [editor]
  {:post [(set? %)]}
  (or (get editor :selection) #{}))

(defn set-selection [editor selection]
  {:pre [(set? selection)]}
  (assoc editor :selection selection))

(defn get-focused-form-id [editor]
  (get editor :focused-form-id))

(defn set-focused-form-id [editor form-id]
  {:pre [form-id]}
  (assoc editor :focused-form-id form-id))

(defn set-render-state [editor render-state]
  (assoc-in editor [:render-state] render-state))

(defn get-id [editor]
  {:post [(pos? %)]}
  (:id editor))

(defn get-editing-set [editor]
  {:post [(set? %)]}
  (or (:editing editor) #{}))

(defn set-editing-set [editor editing-set]
  {:pre [(or (nil? editing-set) (set? editing-set))]}
  (assoc editor :editing (or editing-set #{})))

(defn editing? [editor]
  (let [editing (:editing editor)]
    (and editing (not (empty? editing)))))

(defn transform-parse-tree [editor transformation]
  (let [new-parse-tree (transformation (get-parse-tree editor))]
    (set-parse-tree editor new-parse-tree)))

(defn get-top-level-locs [editor]
  (let [top-loc (zip-utils/make-zipper (get-parse-tree editor)) ; root "forms" node
        first-top-level-form-loc (zip/down top-loc)]
    (zip-utils/collect-all-right first-top-level-form-loc))) ; TODO: here we should use explicit zipping policy

(defn loc-id [loc]
  (:id (zip/node loc)))

(defn loc-id? [id loc]
  (= (loc-id loc) id))

(defn find-node-loc [editor node-id]
  (let [parse-tree (get-parse-tree editor)
        root-loc (zip-utils/make-zipper parse-tree)
        node-loc (findz/find-depth-first root-loc (partial loc-id? node-id))]
    node-loc))

(defn find-node [editor node-id]
  (let [node-loc (find-node-loc editor node-id)]
    (assert (zip-utils/valid-loc? node-loc))
    (zip/node node-loc)))

(defn transfer-sticky-attrs [old new]
  (let [sticky-bits (select-keys old [:id])]
    (merge new sticky-bits)))

(defn empty-value? [value]
  (let [value (node/sexpr value)]
    (if (or (symbol? value) (keyword? value))
      (empty? (node/string value)))))

(defn commit-value-to-loc [loc node-id new-node]
  {:pre [(:id new-node)]}
  (let [node-loc (findz/find-depth-first loc (partial loc-id? node-id))]
    (assert (zip-utils/valid-loc? node-loc))
    (if-not (empty-value? new-node)
      (z/edit node-loc (fn [old-node] (transfer-sticky-attrs old-node new-node)))
      (z/remove node-loc))))

(defn delete-whitespaces-and-newlines-after-loc [loc]
  (loop [cur-loc loc]
    (let [possibly-white-space-loc (z/right cur-loc)]
      (if (and (zip-utils/valid-loc? possibly-white-space-loc) (node/whitespace? (zip/node possibly-white-space-loc)))
        (recur (z/remove possibly-white-space-loc))
        cur-loc))))

(defn delete-whitespaces-and-newlines-before-loc [loc]
  (loop [cur-loc loc]
    (let [possibly-white-space-loc (z/left cur-loc)]
      (if (and (zip-utils/valid-loc? possibly-white-space-loc) (node/whitespace? (zip/node possibly-white-space-loc)))
        (let [loc-after-removal (z/remove possibly-white-space-loc)
              next-step-loc (z/next loc-after-removal)]
          (recur next-step-loc))
        cur-loc))))

(defn delete-node-loc [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial loc-id? node-id))]
    (assert (zip-utils/valid-loc? node-loc))
    (z/remove (delete-whitespaces-and-newlines-before-loc node-loc))))

(defn parse-tree-transformer [f]
  (fn [parse-tree]
    (if-let [result (f (zip-utils/make-zipper parse-tree))]
      (z/root result)
      parse-tree)))

(defn delete-node [editor node-id]
  (transform-parse-tree editor (parse-tree-transformer (partial delete-node-loc node-id))))

(defn commit-node-value [editor node-id value]
  {:pre [node-id value]}
  (let [old-root (get-parse-tree editor)
        root-loc (zip-utils/make-zipper old-root)
        modified-loc (commit-value-to-loc root-loc node-id value)]
    (if-not modified-loc
      editor
      (set-parse-tree editor (zip/root modified-loc)))))

(defn insert-values-after-node-loc [node-id values loc]
  (let [node-loc (findz/find-depth-first loc (partial loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        inserter (fn [loc val] {:pre [(:id val)]} (z/insert-right loc val))]
    (reduce inserter node-loc (reverse values))))

(defn insert-values-before-node-loc [node-id values loc]
  (let [node-loc (findz/find-depth-first loc (partial loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        inserter (fn [loc val] {:pre [(:id val)]}  (z/insert-left loc val))]
    (reduce inserter node-loc values)))

(defn insert-values-after-node [editor node-id values]
  (transform-parse-tree editor (parse-tree-transformer (partial insert-values-after-node-loc node-id values))))

(defn insert-values-before-node [editor node-id values]
  (transform-parse-tree editor (parse-tree-transformer (partial insert-values-before-node-loc node-id values))))

(defn get-render-infos [editor]
  (get-in editor [:render-state :forms]))

(defn get-render-order [editor]
  (get-in editor [:render-state :order]))

(defn set-render-infos [editor render-infos]
  (assoc-in editor [:render-state :forms] render-infos))

(defn get-render-info-by-id [editor form-id]
  (get (get-render-infos editor) form-id))

(defn get-focused-render-info [editor]
  {:post [%]}
  (get-render-info-by-id editor (get-focused-form-id editor)))

(defn get-input-text [editor]
  (let [text (get editor :text)]
    (assert text)
    text))

(defn get-output-text [editor]
  (node/string (get-parse-tree editor)))

(defn can-edit-node? [editor node-id]
  (let [node-loc (find-node-loc editor node-id)]
    (if (zip-utils/valid-loc? node-loc)
      (= (:tag (z/node node-loc) :token)))))

(defn can-edit-selection? [editor]
  (every? (partial can-edit-node? editor) (get-selection editor)))

(defn get-top-level-form-ids [editor]
  (get-render-order editor))

(defn get-first-selectable-token-id-for-form [editor form-id]
  {:post [(number? %)]}
  (let [render-info (get-render-info-by-id editor form-id)
        _ (assert render-info)
        first-line (second (first (:spatial-web render-info)))
        _ (assert (vector? first-line))]
    (:id (first first-line))))

(defn get-last-selectable-token-id-for-form [editor form-id]
  {:post [(number? %)]}
  (let [render-info (get-render-info-by-id editor form-id)
        _ (assert render-info)
        last-line (second (last (:spatial-web render-info)))
        _ (assert (vector? last-line))]
    (:id (peek last-line))))

(defn prepare-placeholder-node []
  (parser/assoc-node-id (token-node "" "")))

(defn prepare-newline-node []
  (parser/assoc-node-id (newline-node "\n")))

(defn prepare-token-node [s]
  (parser/assoc-node-id (token-node (symbol s) s)))

(defn doall-specified-forms [editor f selector]
  (doall
    (for [loc (get-top-level-locs editor)]
      (if (helpers/selector-matches? selector (loc-id loc))
        (f loc)))))
