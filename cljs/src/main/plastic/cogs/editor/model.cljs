(ns plastic.cogs.editor.model
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.findz :as findz]
            [rewrite-clj.node.protocols :as node]
            [clojure.walk :refer [prewalk]]
            [plastic.cogs.editor.parser.utils :as parser]
            [plastic.util.helpers :as helpers]
            [plastic.util.zip :as zip-utils]
            [rewrite-clj.node.token :refer [token-node]]
            [clojure.zip :as z]))

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

(defn loc-sticker? [sticker-value loc]
  (= (:sticker (zip/node loc)) sticker-value))

(defn find-node-loc [editor node-id]
  (let [parse-tree (get-parse-tree editor)
        root-loc (zip-utils/make-zipper parse-tree)
        node-loc (findz/find-depth-first root-loc (partial loc-id? node-id))]
    node-loc))

(defn find-node [editor node-id]
  (let [node-loc (find-node-loc editor node-id)]
    (assert (zip-utils/valid-loc? node-loc))
    (zip/node node-loc)))

(defn set-node-sticker [editor node-id sticker-value]
  (let [node-loc (find-node-loc editor node-id)
        _ (assert (zip-utils/valid-loc? node-loc))
        modified-loc (z/edit node-loc (fn [node] (assoc node :sticker sticker-value)))
        modified-parse-tree (zip/root modified-loc)]
    (set-parse-tree editor modified-parse-tree)))

(defn remove-node-sticker-by-id [editor node-id]
  (let [node-loc (find-node-loc editor node-id)
        _ (assert (zip-utils/valid-loc? node-loc))
        modified-loc (z/edit node-loc (fn [node] (dissoc node :sticker)))
        modified-parse-tree (zip/root modified-loc)]
    (set-parse-tree editor modified-parse-tree)))

(defn find-node-loc-with-sticker [editor sticker-value]
  (let [parse-tree (get-parse-tree editor)
        root-loc (zip-utils/make-zipper parse-tree)
        node-loc (findz/find-depth-first root-loc (partial loc-sticker? sticker-value))]
    node-loc))

(defn find-node-with-sticker [editor sticker-value]
  (let [node-loc (find-node-loc-with-sticker editor sticker-value)]
    (assert (zip-utils/valid-loc? node-loc))
    (zip/node node-loc)))

(defn remove-node-sticker [editor sticker-value]
  (let [node-with-sticker (find-node-with-sticker editor sticker-value)]
    (remove-node-sticker-by-id editor (:id node-with-sticker))))

(defn transfer-sticky-attrs [old new]
  (let [{:keys [id sticker]} old
        new-with-id (if id (assoc new :id id) new)
        new-with-id-and-sticker (if sticker (assoc new-with-id :sticker sticker) new-with-id)]
    new-with-id-and-sticker))

(defn commit-value-to-loc [loc node-id value]
  (let [node-loc (findz/find-depth-first loc (partial loc-id? node-id))]
    (assert (zip-utils/valid-loc? node-loc))
    (if (not= (node/sexpr (zip/node node-loc)) value)
      (z/edit node-loc (fn [prev] (transfer-sticky-attrs prev value))))))

(defn commit-node-value [editor node-id value]
  {:pre [node-id value]}
  (let [old-root (get-parse-tree editor)
        root-loc (zip-utils/make-zipper old-root)
        modified-loc (commit-value-to-loc root-loc node-id value)]
    (if-not modified-loc
      editor
      (let [parse-tree (parser/make-nodes-unique (zip/root modified-loc))
            id-shift (- (:id parse-tree) (:id old-root))]
        (-> editor
          (set-parse-tree parse-tree)
          (shift-selections id-shift))))))

(defn insert-values-after-loc [loc node-id values]
  (let [node-loc (findz/find-depth-first loc (partial loc-id? node-id))
        inserter (fn [loc val] (z/insert-right loc val))]
    (assert (zip-utils/valid-loc? node-loc))
    (reduce inserter node-loc values)))

(defn insert-values-after-node [editor node-id values]
  (let [old-root (get-parse-tree editor)
        root-loc (zip-utils/make-zipper old-root)
        modified-loc (insert-values-after-loc root-loc node-id values)]
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

(defn can-edit-node? [editor node-id]
  (let [node-loc (find-node-loc editor node-id)]
    (if (zip-utils/valid-loc? node-loc)
      (= (:tag (z/node node-loc) :token)))))

(defn can-edit-focused-selection? [editor]
  (every? (partial can-edit-node? editor) (get-focused-selection editor)))

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

(defn prepare-placeholder-node []
  (token-node "" ""))