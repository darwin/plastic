(ns plastic.cogs.editor.parser.core
  (:require [plastic.frame.core :refer [subscribe register-handler]]
            [plastic.schema.paths :as paths]
            [rewrite-clj.parser :as parser]
            [rewrite-clj.node :as node])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]
                   [plastic.macros.glue :refer [react! dispatch]]))

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

(defn parse-source [editors [editor-id text]]
  (let [parse-tree (parser/parse-string-all text)
        unique-parse-tree (make-nodes-unique parse-tree)]
    (dispatch :editor-set-parse-tree editor-id unique-parse-tree))
  editors)

(defn set-parse-tree [editors [editor-id parse-tree]]
  (assoc-in editors [editor-id :parse-tree] parse-tree))

; ----------------------------------------------------------------------------------------------------------------
; register handlers

(register-handler :editor-parse-source paths/editors-path parse-source)
(register-handler :editor-set-parse-tree paths/editors-path set-parse-tree)