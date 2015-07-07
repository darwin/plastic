(ns quark.cogs.editor.analysis.symbols
  (:require [rewrite-clj.node :as node])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn matching-local? [node [decl-node after-id]]
  (or
    (identical? node decl-node)
    (and
      (> (:id node) after-id)
      (= (node/string decl-node) (node/string node)))))

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

(defn resolve-symbol [[node info]]
  (if (= (node/tag node) :token)
    (if-let [declaration-scope (find-symbol-declaration node info)]
      {node (assoc info :declaration-scope declaration-scope)}
      {node info})
    {node info}))

(defn analyze-symbols [_ info]
  (into {} (map resolve-symbol info)))
