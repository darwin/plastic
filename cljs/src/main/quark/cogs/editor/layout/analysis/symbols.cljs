(ns quark.cogs.editor.layout.analysis.symbols
  (:require [rewrite-clj.node :as node])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn matching-local? [node [decl-node after-id]]
  (if (fn? decl-node)
    (decl-node node)
    (or
      (identical? node decl-node)
      (and
        (> (:id node) after-id)
        (= (node/string node) (node/string decl-node))))))

(defn find-symbol-declaration [node scope-info]
  (if scope-info
    (let [locals (get-in scope-info [:scope :locals])
          matching-locals (filter (partial matching-local? node) locals)
          hit-count (count matching-locals)
          best-node (first (last matching-locals))]
      (if-not (= hit-count 0)
        (merge (:scope scope-info)
          {:shadows hit-count}
          (if (identical? best-node node)
            {:decl? true}))
        (find-symbol-declaration node (:parent-scope scope-info))))))

(defn resolve-symbol [nodes record]
  (let [[id info] record
        node (get nodes id)]
    (if (= (node/tag node) :token)
      (if-let [declaration-scope (find-symbol-declaration node info)]
        {id (assoc info :declaration-scope declaration-scope)}
        record)
      record)))

(defn analyze-symbols [nodes info]
  (into {} (map (partial resolve-symbol nodes) info)))
