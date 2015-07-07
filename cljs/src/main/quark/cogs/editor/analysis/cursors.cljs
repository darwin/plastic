(ns quark.cogs.editor.analysis.cursors
  (:require [quark.util.helpers :as helpers]
            [rewrite-clj.node :as node]
            [rewrite-clj.node.stringz :refer [StringNode]]
            [rewrite-clj.node.token :refer [TokenNode]]
            [quark.cogs.editor.cursor :as cursor]
            [quark.cogs.editor.utils :as utils]
            [rewrite-clj.zip :as zip])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn analyze-cursors [editor info]
  (let [cursor-loc (cursor/get-cursor-loc editor)
        cursor-node (zip/node cursor-loc)]
    (helpers/deep-merge info
      {cursor-node {:cursor true}})))

