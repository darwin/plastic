(ns plastic.worker.editor.parser.top-node
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [rewrite-clj.node.protocols :as node]))

(defrecord TopNode [children]
  node/Node
  (tag [_]
    :top)
  (printable-only? [_]
    false)
  (sexpr [_]
    (let [es (node/sexprs children)]
      (if (next es)
        (list* 'do es)
        (first es))))
  (length [_]
    (node/sum-lengths children))
  (string [_]
    (node/concat-strings children)) ; TODO: implement adding two extra linebreaks after every child but last

  node/InnerNode
  (inner? [_]
    true)
  (children [_]
    children)
  (replace-children [this children']
    (assoc this :children children'))

  Object
  (toString [this]
    (node/string this)))

(defn top-node
  [children]
  (->TopNode children))
