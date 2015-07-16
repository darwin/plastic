(ns plastic.cogs.editor.layout.code
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [clojure.zip :as z]
            [plastic.cogs.editor.layout.utils :as layout-utils]
            [plastic.util.zip :as zip-utils]
            [plastic.util.helpers :as helpers]
            [plastic.cogs.editor.layout.utils :as utils]))

(defonce ^:dynamic *line-id* 0)

(defn next-line-id! []
  (set! *line-id* (inc *line-id*))
  *line-id*)

(defn reset-line-id! []
  (set! *line-id* 0))

(defn current-line-id []
  *line-id*)

(defn code-related? [loc]
  (let [node (z/node loc)]
    (or (node/linebreak? node) (not (node/whitespace? node)))))

(def zip-down (partial zip-utils/zip-down code-related?))
(def zip-right (partial zip-utils/zip-right code-related?))

(defn collect-all-right [loc]
  (take-while zip-utils/valid-loc? (iterate zip-right loc)))

(defn child-locs [loc]
  (collect-all-right (zip-down loc)))

(defn build-node-code-render-tree-node [depth loc]
  (let [node (zip/node loc)
        node-id (:id node)
        tag (node/tag node)
        skip? (or (utils/is-doc? loc) (utils/is-whitespace-or-nl-after-doc? loc))]
    (if-not skip?
      (merge
        {:id    node-id
         :tag   tag
         :depth depth
         :line  (current-line-id)}
        (when (or (node/linebreak? node) (node/comment? node)) ; comments have newlines embedded
          (next-line-id!)
          {:tag :newline})
        (if (node/inner? node)
          {:children (keep (partial build-node-code-render-tree-node (inc depth)) (child-locs loc))}
          {:text (node/string node)})
        (if (layout-utils/is-selectable? tag) {:selectable? true})
        (if (layout-utils/string-node? node) {:text (layout-utils/prepare-string-for-display (node/string node)) :type :string})
        (if (layout-utils/keyword-node? node) {:text (helpers/strip-colon (node/string node)) :type :keyword})))))

(defn build-code-render-tree [loc]
  (reset-line-id!)
  {:tag      :code
   :children [(doall (build-node-code-render-tree-node 0 loc))]})