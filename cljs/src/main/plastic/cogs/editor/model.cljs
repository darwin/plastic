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

(defn bake-loc [focused selection editing loc]
  (z/edit loc (fn [node]
                (let [id (:id node)]
                  (merge node {:focused?  (contains? focused id)
                               :selected? (contains? selection id)
                               :editing?  (contains? editing id)})))))

(defn bake-ids-into-parse-tree [parse-tree focused selection editing]
  (let [baker (partial bake-loc focused selection editing)]
    (loop [loc (zip-utils/make-zipper parse-tree)]
      (if (z/end? loc)
        (z/root loc)
        (recur (z/next (baker loc)))))))

(defn accum-if-matches [accum node key]
  (if (get node key)
    (update accum key (fn [old] (conj old (:id node))))
    accum))

(defn accum-node [accum node]
  (-> accum
    (accum-if-matches node :focused?)
    (accum-if-matches node :selected?)
    (accum-if-matches node :editing?)))

(defn unbake-loc [accum loc]
  [(accum-node accum (z/node loc))
   (z/edit loc (fn [node]
                 (-> node
                   (dissoc :focused?)
                   (dissoc :selected?)
                   (dissoc :editing?))))])

(defn unbake-ids-from-parse-tree [parse-tree]
  (loop [loc (zip-utils/make-zipper parse-tree)
         accum {}]
    (if (z/end? loc)
      [(z/root loc) accum]
      (let [[new-accum new-loc] (unbake-loc accum loc)]
        (recur (z/next new-loc) new-accum)))))

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

; arbitrary transformation of parse tree with reindexing of important ids in the editor
(defn transform-parse-tree [editor transformation]
  (let [[reindexed-parse-tree new-ids] (-> (get-parse-tree editor)
                                         (bake-ids-into-parse-tree
                                           #{(get-focused-form-id editor)}
                                           (get-selection editor)
                                           (get-editing-set editor))
                                         (transformation)
                                         (parser/make-nodes-unique)
                                         (unbake-ids-from-parse-tree))]
    (-> editor
      (set-parse-tree reindexed-parse-tree)
      (set-editing-set (set (:editing? new-ids)))
      (set-selection (set (:selected? new-ids)))
      (set-focused-form-id (first (:focused? new-ids))))))

(defn get-top-level-locs [editor]
  (let [top-loc (zip-utils/make-zipper (get-parse-tree editor)) ; root "forms" node
        first-top-level-form-loc (zip/down top-loc)]
    (zip-utils/collect-all-right first-top-level-form-loc))) ; TODO: here we should use explicit zipping policy

(defn loc-id [loc]
  (:id (zip/node loc)))

(defn loc-id? [id loc]
  (= (loc-id loc) id))

(defn node-add-sticker [node sticker-value]
  (let [old-stickers (or (:stickers node) [])]
    (assoc node :stickers (conj old-stickers sticker-value))))

(defn node-remove-sticker [node sticker-value]
  (let [old-stickers (or (:stickers node) [])
        new-stickers (remove #(= % sticker-value) old-stickers)]
    (if (empty? new-stickers)
      (dissoc node :stickers)
      (assoc node :stickers new-stickers))))

(defn node-has-sticker? [node sticker-value]
  (some #(= % sticker-value) (:stickers node)))

(defn loc-has-sticker? [sticker-value loc]
  (node-has-sticker? (zip/node loc) sticker-value))

(defn find-node-loc [editor node-id]
  (let [parse-tree (get-parse-tree editor)
        root-loc (zip-utils/make-zipper parse-tree)
        node-loc (findz/find-depth-first root-loc (partial loc-id? node-id))]
    node-loc))

(defn find-node [editor node-id]
  (let [node-loc (find-node-loc editor node-id)]
    (assert (zip-utils/valid-loc? node-loc))
    (zip/node node-loc)))

(defn add-sticker-on-node [editor node-id sticker-value]
  (let [node-loc (find-node-loc editor node-id)
        _ (assert (zip-utils/valid-loc? node-loc))
        modified-loc (z/edit node-loc (fn [node] (node-add-sticker node sticker-value)))
        modified-parse-tree (zip/root modified-loc)]
    (set-parse-tree editor modified-parse-tree)))

(defn remove-node-sticker-at-loc [editor node-loc sticker-value]
  (let [modified-loc (z/edit node-loc (fn [node] (node-remove-sticker node sticker-value)))
        modified-parse-tree (zip/root modified-loc)]
    (set-parse-tree editor modified-parse-tree)))

(defn find-node-loc-with-sticker [editor sticker-value]
  (let [parse-tree (get-parse-tree editor)
        root-loc (zip-utils/make-zipper parse-tree)
        node-loc (findz/find-depth-first root-loc (partial loc-has-sticker? sticker-value))]
    node-loc))

(defn find-node-with-sticker [editor sticker-value]
  (let [node-loc (find-node-loc-with-sticker editor sticker-value)]
    (assert (zip-utils/valid-loc? node-loc))
    (zip/node node-loc)))

(defn remove-sticker [editor sticker-value]
  (let [node-loc-with-sticker (find-node-loc-with-sticker editor sticker-value)]
    (if (zip-utils/valid-loc? node-loc-with-sticker)
      (remove-node-sticker-at-loc editor node-loc-with-sticker sticker-value)
      editor)))

(defn transfer-sticky-attrs [old new]
  (let [sticky-bits (select-keys old [:id :stickers :editing? :selected? :focused?])]
    (merge new sticky-bits)))

(defn empty-value? [value]
  (let [value (node/sexpr value)]
    (if (or (symbol? value) (keyword? value))
      (empty? (node/string value)))))

(defn commit-value-to-loc [loc node-id new-node]
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
        inserter (fn [loc val] (z/insert-right loc val))]
    (assert (zip-utils/valid-loc? node-loc))
    (reduce inserter node-loc (reverse values))))

(defn insert-values-after-node [editor node-id values]
  (transform-parse-tree editor (parse-tree-transformer (partial insert-values-after-node-loc node-id values))))

(defn get-render-infos [editor]
  (get-in editor [:render-state :forms]))

(defn set-render-infos [editor render-infos]
  (assoc-in editor [:render-state :forms] render-infos))

(defn get-render-info-by-id [editor form-id]
  (helpers/get-by-id form-id (get-render-infos editor)))

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

(defn prepare-newline-node []
  (newline-node "\n"))

(defn prepare-token-node [s]
  (token-node (symbol s) s))