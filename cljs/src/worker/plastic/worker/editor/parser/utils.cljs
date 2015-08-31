(ns plastic.worker.editor.parser.utils
  (:require-macros [plastic.logging :refer [log info warn error group group-end]]
                   [plastic.common :refer [process]])
  (:require [rewrite-clj.node :as node]
            [rewrite-clj.parser :as rewrite-cljs]
            [rewrite-clj.node.forms :refer [forms-node]]
            [plastic.util.zip :as zip-utils :refer [valid-loc?]]
            [plastic.worker.editor.parser.top-node :refer [top-node]]
            [clojure.zip :as z]))

(defonce ^:dynamic node-id 0)

(defn next-node-id! []
  (set! node-id (inc node-id))
  node-id)

(defn assoc-node-id [node]
  (assoc node :id (next-node-id!)))

(defn make-nodes-unique [node]
  (let [unique-node (assoc-node-id node)]
    (if (node/inner? unique-node)
      (node/replace-children unique-node (map make-nodes-unique (node/children unique-node)))
      unique-node)))

; -------------------------------------------------------------------------------------------------------------------

(defn space? [node]
  (and (node/whitespace? node) (not (node/linebreak? node))))

(defn linebreak-between-comments? [loc]
  (if (node/linebreak? (z/node loc))
    (if-let [left-1 (z/left loc)]
      (if (node/comment? (z/node left-1))
        (if-let [right-1 (z/right loc)]
          (node/comment? (z/node right-1)))))))

(defn comment-after-comment-with-linebreak? [loc]
  (if (node/comment? (z/node loc))
    (if-let [left-1 (z/left loc)]
      (if (node/linebreak? (z/node left-1))
        (if-let [left-2 (z/left left-1)]
          (node/comment? (z/node left-2)))))))

(defn comment-not-after-linebreak? [loc]
  (if (node/comment? (z/node loc))
    (if-let [left-1 (z/left loc)]
      (not (node/linebreak? (z/node left-1))))))

(defn join? [loc]
  (let [node (z/node loc)]
    (or
      (not (valid-loc? (z/left loc)))
      (space? node)
      ; stitching comments together
      (comment-not-after-linebreak? loc)
      (linebreak-between-comments? loc)
      (comment-after-comment-with-linebreak? loc))))

(defn make-chunks [loc]
  (loop [chunks [[]]
         loc loc]
    (if-not (valid-loc? loc)
      chunks
      (let [node (z/node loc)]
        ;(log "!" (node/tag node) (map count chunks) (join? loc) (comment-after-comment? loc))
        (if (join? loc)
          (recur (assoc chunks (dec (count chunks)) (conj (last chunks) node)) (z/right loc))
          (recur (conj chunks [node]) (z/right loc)))))))

(defn single-linebreak-chunk? [chunk]
  (and
    (= (count chunk) 1)
    (node/linebreak? (first chunk))))

; for each chunk, remove single-linebreak-containing chunks after it (if exists)
(defn filter-implicit-linebreaks [chunks]
  (let [chunk-groups (partition-by single-linebreak-chunk? chunks)]
    (mapcat (fn [chunk-group]
              (if (single-linebreak-chunk? (first chunk-group))
                (drop 1 chunk-group)
                chunk-group))
      chunk-groups)))

(defn make-forms [chunks]
  (map (fn [chunk] (forms-node chunk)) chunks))

; -------------------------------------------------------------------------------------------------------------------

(defn parse [text]
  (-> text
    rewrite-cljs/parse-string-all
    zip-utils/make-zipper
    z/down
    make-chunks
    filter-implicit-linebreaks
    make-forms
    top-node
    make-nodes-unique))
