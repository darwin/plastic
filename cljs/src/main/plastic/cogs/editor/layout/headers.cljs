(ns plastic.cogs.editor.layout.headers
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.node :as node]
            [clojure.zip :as z]
            [plastic.util.zip :as zip-utils]
            [plastic.cogs.editor.layout.utils :as utils]))

(defn header-item [loc]
  (let [node (z/node loc)
        text (node/string node)]
    (if text
      {:id   (:id node)
       :name text})))

(defn structure? [loc]
  (let [node (z/node loc)]
    (not (or (node/whitespace? node) (node/comment? node)))))

(def zip-next (partial zip-utils/zip-next structure?))

(defn find-header-locs [loc]
  (filter utils/is-def-name? (take-while zip-utils/valid-loc? (iterate zip-next loc))))

(defn build-headers-render-tree [loc]
  (let [header-locs (find-header-locs loc)]
    {:tag      :headers
     :children (keep header-item header-locs)}))