(ns quark.cogs.editor.selections
  (:require [rewrite-clj.zip :as zip]
            [rewrite-clj.node :as node]
            [quark.cogs.editor.utils :refer [make-zipper path->loc loc->path] :as utils]
            [clojure.zip :as z])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]
                   [quark.macros.glue :refer [react! dispatch]]))

; editor's :selections is a vector of selected node-ids
