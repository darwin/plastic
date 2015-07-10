(ns quark.cogs.editor.model
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.findz :as findz]
            [rewrite-clj.zip.editz :as editz]
            [quark.cogs.editor.parser :as parser]
            [rewrite-clj.node.protocols :as node]
            [clojure.walk :refer [prewalk]]
            [quark.cogs.editor.layout.utils :as utils]))

(defn get-parse-tree [editor]
  (let [parse-tree (get editor :parse-tree)]
    (assert parse-tree)
    parse-tree))

(defn set-parse-tree [editor parse-tree]
  {:pre [(= (node/tag parse-tree) :forms)]}
  (assoc editor :parse-tree parse-tree))

(defn get-selections [editor]
  (or (get editor :selections) {}))

(defn set-selections [editor selections]
  (assoc editor :selections selections))

(defn shift-selections [editor shift]
  (let [selections (get-selections editor)
        shift #(if (number? %) (+ % shift) %)
        shifter (fn [x] (shift x))
        shifted-selections (prewalk shifter selections)]
    (set-selections editor shifted-selections)))

(defn get-focused-form-id [editor]
  (get-in editor [:selections :focused-form-id]))

(defn get-focused-selection [editor]
  (let [selections (get-selections editor)
        focused-form-id (get-focused-form-id editor)]
    (get selections focused-form-id)))

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

(defn get-top-level-form-locs [editor]
  (let [top-loc (utils/make-zipper (get-parse-tree editor)) ; root "forms" node
        first-top-level-form-loc (zip/down top-loc)]
    (utils/collect-all-right first-top-level-form-loc)))    ; TODO: here we should use explicit zipping policy

(defn loc-id? [id loc]
  (= (:id (zip/node loc)) id))

(defn commit-value-to-loc [loc node-id value]
  (let [node-loc (findz/find-depth-first loc (partial loc-id? node-id))
        new-value (symbol value)]
    (assert (utils/valid-loc? node-loc))
    (if (not= (node/sexpr (zip/node node-loc)) new-value)
      (editz/edit node-loc (fn [_prev] new-value)))))

(defn commit-node-value [editor node-id value]
  {:pre [node-id value]}
  (let [old-root (get-parse-tree editor)
        modified-loc (commit-value-to-loc (utils/make-zipper old-root) node-id value)]
    (if-not modified-loc
      editor
      (let [parse-tree (parser/make-nodes-unique (zip/root modified-loc))
            id-shift (- (:id parse-tree) (:id old-root))]
        (-> editor
          (set-parse-tree parse-tree)
          (shift-selections id-shift))))))