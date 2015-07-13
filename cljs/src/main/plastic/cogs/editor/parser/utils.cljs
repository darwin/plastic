(ns plastic.cogs.editor.parser.utils
                        (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
                        (:require [rewrite-clj.node :as node]))

(defonce ^:dynamic node-id 0)

(defn next-node-id! []
  (set! node-id (+ node-id 10))                             ; this is a hack to allow some virtual render tree node ids without id conflicts
  node-id)

(defn assoc-node-id [node]
  (assoc node :id (next-node-id!)))

(defn make-nodes-unique [node]
  (let [unique-node (assoc-node-id node)]
    (if (node/inner? unique-node)
      (node/replace-children unique-node (map make-nodes-unique (node/children unique-node)))
      unique-node)))