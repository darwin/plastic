(ns quark.cogs.editor.layout.soup
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [quark.cogs.editor.utils :refer [ancestor-count loc->path leaf-nodes make-zipper collect-all-right]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn soup-movement-policy [loc]
  (let [node (zip/node loc)]
    (not (or (node/whitespace? node) (node/comment? node)))))

(defn item-info [loc]
  (let [node (zip/node loc)]
    (if (node/comment? node)
      {:tag   :newline
       :depth (ancestor-count soup-movement-policy loc)
       :path  (loc->path loc)
       :text  ("\n")}
      {:string (zip/string loc)
       :tag    (node/tag node)
       :depth  (ancestor-count soup-movement-policy loc)
       :path   (loc->path loc)})))

(defn build-soup-render-info [node]
  (map item-info (leaf-nodes soup-movement-policy (make-zipper node))))