(ns plastic.cogs.editor.model
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.findz :as findz]
            [rewrite-clj.zip.editz :as editz]
            [rewrite-clj.node.protocols :as node]
            [clojure.walk :refer [prewalk]]
            [plastic.cogs.editor.parser.utils :as parser]
            [plastic.util.helpers :as helpers]
            [plastic.util.zip :as zip-utils]))

(defn parsed? [editor]
  (contains? editor :parse-tree))

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
        shifted-selections (prewalk shift selections)]
    (set-selections editor shifted-selections)))

(defn get-focused-form-id [editor]
  (get-in editor [:selections :focused-form-id]))

(defn set-focused-form-id [editor form-id]
  {:pre [form-id]}
  (assoc-in editor [:selections :focused-form-id] form-id))

(defn get-focused-selection [editor]
  (let [selections (get-selections editor)
        focused-form-id (get-focused-form-id editor)]
    (get selections focused-form-id)))

(defn set-focused-selection [editor selection]
  (let [selections (get-selections editor)
        new-selections (assoc selections (get-focused-form-id editor) selection)]
    (set-selections editor new-selections)))

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

(defn get-top-level-locs [editor]
  (let [top-loc (zip-utils/make-zipper (get-parse-tree editor)) ; root "forms" node
        first-top-level-form-loc (zip/down top-loc)]
    (zip-utils/collect-all-right first-top-level-form-loc))) ; TODO: here we should use explicit zipping policy

(defn loc-id? [id loc]
  (= (:id (zip/node loc)) id))

(defn commit-value-to-loc [loc node-id value]
  (let [node-loc (findz/find-depth-first loc (partial loc-id? node-id))]
    (assert (zip-utils/valid-loc? node-loc))
    (if (not= (node/sexpr (zip/node node-loc)) value)
      (editz/edit node-loc (fn [_prev] value)))))

(defn commit-node-value [editor node-id value]
  {:pre [node-id value]}
  (let [old-root (get-parse-tree editor)
        modified-loc (commit-value-to-loc (zip-utils/make-zipper old-root) node-id value)]
    (if-not modified-loc
      editor
      (let [parse-tree (parser/make-nodes-unique (zip/root modified-loc))
            id-shift (- (:id parse-tree) (:id old-root))]
        (-> editor
          (set-parse-tree parse-tree)
          (shift-selections id-shift))))))

(defn get-render-infos [editor]
  (get-in editor [:render-state :forms]))

(defn set-render-infos [editor render-infos]
  (assoc-in editor [:render-state :forms] render-infos))

(defn get-render-info-by-id [editor form-id]
  (helpers/get-by-id form-id (get-render-infos editor)))

(defn get-focused-render-info [editor]
  (get-render-info-by-id editor (get-focused-form-id editor)))

(defn get-input-text [editor]
  (let [text (get editor :text)]
    (assert text)
    text))

(defn get-output-text [editor]
  (node/string (get-parse-tree editor)))

(defn can-edit-node? [editor render-info node-id]
  {:pre [render-info editor]}
  (let [{:keys [selectables]} render-info
        item (get selectables node-id)
        _ (assert item)]
    (= (:tag item) :token)))                                ; we have also :tree node in selectables

(defn can-edit-focused-selection? [editor]
  (if-let [render-info (get-focused-render-info editor)]
    (every? (partial can-edit-node? editor render-info) (get-focused-selection editor))))

(defn get-top-level-form-ids [editor]
  (let [render-infos (get-render-infos editor)]
    (map :id render-infos)))

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
