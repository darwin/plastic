(ns quark.cogs.editor.layout.soup
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [quark.cogs.editor.utils :refer [ancestor-count loc->path leaf-nodes make-zipper collect-all-right]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(defn emit-token [token info]
  (assoc token :pos info))

(defn build-soup-render-info [tokens layout-info]
  (map (fn [[id info]] (emit-token (get tokens id) info)) layout-info))