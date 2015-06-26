(ns quark.schema.paths
  (:require [quark.frame.middleware :refer [path]])
  (:require-macros [quark.macros.logging :refer [log info warn error group group-end]]))

(def editors [:editors])
(def editors-path (path editors))
