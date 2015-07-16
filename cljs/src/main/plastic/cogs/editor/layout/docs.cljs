(ns plastic.cogs.editor.layout.docs
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.node :as node]
            [plastic.cogs.editor.layout.utils :as layout-utils]
            [plastic.util.zip :as zip-utils]
            [clojure.zip :as z]
            [plastic.cogs.editor.layout.utils :as utils]))

(defn doc-item [loc]
  (let [node (z/node loc)
        text (node/string node)
        {:keys [id]} node]
    (merge {:id          id
            :tag         :token
            :doc?        true
            :selectable? true
            :line        -1
            :text        (layout-utils/prepare-string-for-display text)})))


(defn structure? [loc]
  (let [node (z/node loc)]
    (not (or (node/whitespace? node) (node/comment? node)))))

(def zip-next (partial zip-utils/zip-next structure?))

(defn find-doc-locs [loc]
  (filter utils/is-doc? (take-while zip-utils/valid-loc? (iterate zip-next loc))))

(defn build-docs-render-tree [loc]
  (let [doc-locs (find-doc-locs loc)]
    {:tag      :docs
     :children (map doc-item doc-locs)}))