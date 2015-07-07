(ns quark.cogs.editor.cursor
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [quark.cogs.editor.utils :refer [make-zipper path->loc loc->path] :as utils]
            [clojure.zip :as z])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]))

; editor's :cursor is a path into rewrite-cljs tree

; cursor skips all whitespace and comments
(defn cursor-movement-policy [loc]
  (let [node (zip/node loc)]
    (not (or (node/whitespace? node) (node/comment? node)))))

(def zip-up (partial utils/zip-up cursor-movement-policy))
(def zip-down (partial utils/zip-down cursor-movement-policy))
(def zip-left (partial utils/zip-left cursor-movement-policy))
(def zip-right (partial utils/zip-right cursor-movement-policy))

(defn cursor->loc [cursor root]
  (let [top-loc (make-zipper root)
        loc (path->loc cursor top-loc)]
    (or loc top-loc)))

(defn get-cursor-loc [editor]
  (let [{:keys [cursor parse-tree]} editor
        cursor-loc (cursor->loc cursor parse-tree)]
    cursor-loc))

(defn move-cursor [cursor-loc movement]
  (let [moved-loc (movement cursor-loc)]
    (if (utils/valid-loc? moved-loc)
      moved-loc
      cursor-loc)))

(defn apply-move-cursor [editor movement]
  (let [cursor-loc (get-cursor-loc editor)
        _ (log "cursor at" cursor-loc (zip/string cursor-loc))
        moved-loc (move-cursor cursor-loc movement)
        path (loc->path moved-loc)]
    (log "cursor moved to" moved-loc path)
    (assoc editor :cursor path)))

(defn move-up [editor]
  (apply-move-cursor editor zip-up))

(defn move-down [editor]
  (apply-move-cursor editor zip-down))

(defn move-left [editor]
  (apply-move-cursor editor zip-left))

(defn move-right [editor]
  (apply-move-cursor editor zip-right))
