(ns plastic.cogs.editor.parser.utils
  (:require [rewrite-clj.node :as node])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

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