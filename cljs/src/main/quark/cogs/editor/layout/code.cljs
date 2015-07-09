(ns quark.cogs.editor.layout.code
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.stringz :refer [StringNode]]
            [rewrite-clj.node.keyword :refer [KeywordNode]]
            [quark.cogs.editor.utils :refer [is-selectable? prepare-string-for-display ancestor-count loc->path leaf-nodes make-zipper collect-all-right]]
            [clojure.zip :as z])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defonce ^:dynamic *line-id* 0)

(defn next-line-id! []
  (set! *line-id* (inc *line-id*))
  *line-id*)

(defn reset-line-id! []
  (set! *line-id* 0))

(defn current-line-id []
  *line-id*)

(defn is-whitespace-or-nl-after-def-doc? [analysis loc]
  (let [node (zip/node loc)]
    (if (or (node/whitespace? node) (node/linebreak? node))
      (let [prev (z/left loc)
            prev-node (zip/node prev)
            prev-node-analysis (get analysis (:id prev-node))]
        (:def-doc? prev-node-analysis)))))

(defn layout-affecting-children [loc]
  (let [first (zip/down loc)
        children (take-while (complement nil?)
                   (iterate z/right first))
        interesting? (fn [loc]
                       (let [node (zip/node loc)]
                         (or (node/linebreak? node) (not (node/whitespace? node)))))] ; skip whitespaces but keep line breaks
    (filter interesting? children)))

(defn is-call? [loc]
  (if-let [parent-loc (z/up loc)]
    (if (= (node/tag (zip/node parent-loc)) :list)
      (= loc (first (layout-affecting-children parent-loc))))))

(defn build-node-code-render-info [depth scope-id analysis loc]
  (let [node (zip/node loc)
        node-id (:id node)
        node-analysis (get analysis node-id)
        new-scope-id (get-in node-analysis [:scope :id])
        tag (node/tag node)
        {:keys [declaration-scope def-name? def-doc? cursor editing?]} node-analysis
        {:keys [shadows decl?]} declaration-scope]
    (if (or def-doc? (is-whitespace-or-nl-after-def-doc? analysis loc))
      nil
      (merge
        {:id    node-id
         :tag   tag
         :depth depth
         :line  (current-line-id)}
        (when (or (node/linebreak? node) (node/comment? node)) (next-line-id!) {:type :newline :text "\n"}) ; comments have newlines embedded
        (if (node/inner? node)
          {:children (doall (remove nil? (map (partial build-node-code-render-info (inc depth) new-scope-id analysis) (layout-affecting-children loc))))}
          {:text (node/string node)})
        (if (is-selectable? tag) {:selectable true})
        (if editing? {:editing? true})
        (if (is-call? loc) {:call true})
        (if (instance? StringNode node) {:text (prepare-string-for-display (node/string node)) :type :string})
        (if (instance? KeywordNode node) {:type :keyword})
        (if (not= new-scope-id scope-id) {:scope new-scope-id})
        (if def-name? {:def-name? true})
        (if declaration-scope {:decl-scope (:id declaration-scope)})
        (if cursor {:cursor true})
        (if shadows {:shadows shadows})
        (if decl? {:decl? decl?})))))

(defn build-code-render-info [analysis node]
  (reset-line-id!)
  (doall (build-node-code-render-info 0 nil analysis node)))