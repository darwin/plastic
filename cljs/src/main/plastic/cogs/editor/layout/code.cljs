(ns plastic.cogs.editor.layout.code
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.stringz :refer [StringNode]]
            [rewrite-clj.node.keyword :refer [KeywordNode]]
            [clojure.zip :as z]
            [plastic.cogs.editor.layout.utils :as layout-utils]
            [plastic.util.zip :as zip-utils]
            [plastic.util.helpers :as helpers]))

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

(def zip-up (partial zip-utils/zip-up code-related?))
(def zip-down (partial zip-utils/zip-down code-related?))
(def zip-left (partial zip-utils/zip-left code-related?))
(def zip-right (partial zip-utils/zip-right code-related?))
(def zip-next (partial zip-utils/zip-next code-related?))

(defn collect-all-right [loc]
  (take-while zip-utils/valid-loc? (iterate zip-right loc)))

(defn child-locs [loc]
  (collect-all-right (zip-down loc)))

(defn is-whitespace-or-nl-after-def-doc? [analysis loc]
  (let [node (zip/node loc)]
    (if (node/whitespace? node)
      (let [prev (zip-left loc)]
        (if (zip-utils/valid-loc? prev)
          (let [prev-node (zip/node prev)
                prev-node-analysis (get analysis (:id prev-node))]
            (:def-doc? prev-node-analysis)))))))

(defn is-call? [loc]
  (if-let [parent-loc (zip-up loc)]
    (if (= (zip/tag parent-loc) :list)
      (= loc (z/leftmost loc)))))

(defn build-node-code-render-tree-node [depth scope-id analysis loc]
  (let [node (zip/node loc)
        node-id (:id node)
        node-analysis (get analysis node-id)
        scope (get node-analysis :scope)
        tag (node/tag node)
        {:keys [declaration-scope def-name? def-doc? cursor editing? selectable?]} node-analysis
        {:keys [shadows decl?]} declaration-scope]
    (if-not (or def-doc? (is-whitespace-or-nl-after-def-doc? analysis loc))
      (merge
        {:id    node-id
         :tag   tag
         :depth depth
         :line  (current-line-id)}
        (when (or (node/linebreak? node) (node/comment? node))
          (next-line-id!)
          {:tag :newline})                                  ; comments have newlines embedded
        (if (node/inner? node)
          {:children (keep (partial build-node-code-render-tree-node (inc depth) (:id scope) analysis) (child-locs loc))}
          {:text (node/string node)})
        (if selectable? {:selectable? true})
        (if editing? {:editing? true})
        (if (is-call? loc) {:call true})
        (if (instance? StringNode node) {:text (layout-utils/prepare-string-for-display (node/string node))
                                         :type :string})
        (if (instance? KeywordNode node) {:text (helpers/strip-colon (node/string node))
                                          :type :keyword})
        (if (not= (:id scope) scope-id) {:scope       (:id scope)
                                         :scope-depth (:depth scope)})
        (if def-name? {:def-name? true})
        (if declaration-scope {:decl-scope (:id declaration-scope)})
        (if cursor {:cursor true})
        (if shadows {:shadows shadows})
        (if decl? {:decl? decl?})))))

(defn build-code-render-tree [analysis node]
  (reset-line-id!)
  {:tag      :code
   :children [(doall (build-node-code-render-tree-node 0 nil analysis node))]})