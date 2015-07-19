(ns plastic.cogs.editor.model
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.zip.findz :as findz]
            [rewrite-clj.node :as node]
            [clojure.walk :refer [prewalk]]
            [plastic.cogs.editor.parser.utils :as parser]
            [plastic.util.helpers :as helpers]
            [plastic.cogs.editor.layout.builder :as layout]
            [plastic.util.zip :as zip-utils]
            [rewrite-clj.node.token :refer [token-node]]
            [rewrite-clj.node.whitespace :refer [newline-node]]
            [clojure.zip :as z]))

; a kitchen-sink with helpers for manipulating editor data structure
; also see 'ops' folder for more high-level editor transformations

(defn strip-comments-and-whitespaces-but-keep-linebreaks-policy [loc]
  (let [node (z/node loc)]
    (and (not (node/comment? node)) (or (node/linebreak? node) (not (node/whitespace? node))))))

(def zip-down (partial zip-utils/zip-down strip-comments-and-whitespaces-but-keep-linebreaks-policy))
(def zip-right (partial zip-utils/zip-right strip-comments-and-whitespaces-but-keep-linebreaks-policy))
(def zip-left (partial zip-utils/zip-left strip-comments-and-whitespaces-but-keep-linebreaks-policy))

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

(defn get-cursor [editor]
  {:pre [(set? (or (get editor :cursor) #{}))
         (<= (count (get editor :cursor)) 1)]}
  (first (get editor :cursor)))

(defn cursor-and-selection-are-linked? [editor]
  (let [selection (get-selection editor)]
    (or (empty? selection) (and (= (count selection) 1) (= (first selection) (get-cursor editor))))))

(defn set-selection [editor selection]
  {:pre [(set? selection)]}
  (assoc editor :selection selection))

(defn toggle-selection [editor toggle-set]
  {:pre [(set? toggle-set)]}
  (let [toggler (fn [accum id]
                  (if (contains? accum id)
                    (disj accum id)
                    (conj accum id)))
        old-selection (get-selection editor)
        new-selection (reduce toggler old-selection toggle-set)]
    (set-selection editor new-selection)))

(defn set-cursor [editor cursor]
  (let [new-cursor (if cursor #{cursor} #{})]
    (cond-> editor
      (cursor-and-selection-are-linked? editor) (assoc :selection new-cursor)
      true (assoc :cursor new-cursor))))

(defn get-focused-form-id [editor]
  (get editor :focused-form-id))

(defn set-focused-form-id [editor form-id]
  {:pre [form-id]}
  (assoc editor :focused-form-id form-id))

(defn get-render-state [editor]
  (get editor :render-state))

(defn set-render-state [editor render-state]
  (let [old-render-state (get-render-state editor)]
    (if (not= old-render-state render-state)
      (assoc-in editor [:render-state] render-state)
      editor)))

(defn get-id [editor]
  {:post [(pos? %)]}
  (:id editor))

(defn get-editing [editor]
  {:pre [(set? (:editing editor))
         (<= (count (:editing editor)) 1)]}
  (first (:editing editor)))

(defn set-editing [editor node-id]
  (if node-id
    (assoc editor :editing #{node-id})
    (assoc editor :editing #{})))

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
        inserter (fn [loc val] {:pre [(:id val)]} (z/insert-left loc val))]
    (reduce inserter node-loc values)))

(defn insert-values-after-node [editor node-id values]
  (transform-parse-tree editor (parse-tree-transformer (partial insert-values-after-node-loc node-id values))))

(defn insert-values-before-node [editor node-id values]
  (transform-parse-tree editor (parse-tree-transformer (partial insert-values-before-node-loc node-id values))))

(defn remove-linebreak-before-node-loc [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        first-linebreak (first (filter #(node/linebreak? (zip/node %)) (take-while zip-utils/valid-loc? (iterate z/prev node-loc))))]
    (if first-linebreak
      (z/remove first-linebreak))))

(defn remove-linebreak-before-node [editor node-id]
  (transform-parse-tree editor (parse-tree-transformer (partial remove-linebreak-before-node-loc node-id))))

(defn remove-right-siblink-of-loc [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        right-loc (zip-right node-loc)]
    (if right-loc
      (z/remove right-loc))))

(defn remove-right-siblink [editor node-id]
  (transform-parse-tree editor (parse-tree-transformer (partial remove-right-siblink-of-loc node-id))))

(defn remove-left-siblink-of-loc [node-id loc]
  (let [node-loc (findz/find-depth-first loc (partial loc-id? node-id))
        _ (assert (zip-utils/valid-loc? node-loc))
        left-loc (zip-left node-loc)]
    (if left-loc
      (z/remove left-loc))))

(defn remove-left-siblink [editor node-id]
  (transform-parse-tree editor (parse-tree-transformer (partial remove-left-siblink-of-loc node-id))))

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

(defn set-focused-render-info [editor render-info]
  {:post [%]}
  (assoc-in editor [:render-state :forms (get-focused-form-id editor)] render-info))

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

(defn can-edit-cursor? [editor]
  (can-edit-node? editor (get-cursor editor)))

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

(defn make-render-tree-zipper [root]
  (z/zipper
    (fn [] true)
    #(seq (:children %))
    (fn [node children] (assoc node :children children))
    root))

(defn update-render-tree-node-in-focused-form [editor node-id value-node]
  (let [focused-render-info (get-focused-render-info editor)
        render-info-loc (make-render-tree-zipper (:render-tree focused-render-info))
        node-loc (first (filter #(= (:id (z/node %)) node-id) (take-while zip-utils/valid-loc? (iterate z/next render-info-loc))))]
    (if node-loc
      (let [modified-tree (z/root (z/edit node-loc (fn [old-node] (assoc old-node :text (layout/prepare-node-text value-node)))))
            modified-focused-render-info (assoc focused-render-info :render-tree modified-tree)]
        (set-focused-render-info editor modified-focused-render-info))
      editor)))

(defn set-analysis-for-form [editor form-id analysis]
  (assoc-in editor [:analysis form-id] analysis))

(defn get-analysis-for-form [editor form-id]
  (get-in editor [:analysis form-id]))

(defn get-layout-for-form [editor form-id]
  (get-in editor [:layout form-id]))

(defn set-layout-for-form [editor form-id new-layout]
  (let [old-layout (get-layout-for-form editor form-id)
        updated-layout (helpers/overwrite-map old-layout new-layout)]
    (assoc-in editor [:layout form-id] updated-layout)))

(defn get-selectables-for-form [editor form-id]
  (get-in editor [:selectables form-id]))

(defn set-selectables-for-form [editor form-id new-selectables]
  (let [old-selectables (get-selectables-for-form editor form-id)
        updated-selectables (helpers/overwrite-map old-selectables new-selectables)]
    (assoc-in editor [:selectables form-id] updated-selectables)))

(defn get-spatial-web-for-form [editor form-id]
  (get-in editor [:spatial-web form-id]))

(defn set-spatial-web-for-form [editor form-id new-spatial-web]
  (let [old-spatial-web (get-spatial-web-for-form editor form-id)
        updated-spatial-web (helpers/overwrite-map old-spatial-web new-spatial-web)]
    (assoc-in editor [:spatial-web form-id] updated-spatial-web)))

(defn get-structural-web-for-form [editor form-id]
  (get-in editor [:structural-web form-id]))

(defn set-structural-web-for-form [editor form-id new-structural-web]
  (let [old-structural-web (get-structural-web-for-form editor form-id)
        updated-structural-web (helpers/overwrite-map old-structural-web new-structural-web)]
    (assoc-in editor [:structural-web form-id] updated-structural-web)))

(defn get-cached-form-node [editor form-id]
  (get-in editor [:previously-layouted-forms form-id]))

(defn set-cached-form-node [editor form-node]
  (assoc-in editor [:previously-layouted-forms (:id form-node)] form-node))

