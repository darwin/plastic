(ns quark.cogs.editor.cursor
  (:require [rewrite-clj.zip :as rzip]
            [rewrite-clj.node :as rnode]
            [rewrite-clj.zip.whitespace :as ws]
            [quark.cogs.editor.analyzer :refer [analyze-full]]
            [clojure.zip :as z])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

; cursor is a path into rewrite-cljs tree and integer specifying selection length




