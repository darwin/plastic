(ns plastic.schema.paths
  (:require [plastic.frame.middleware :refer [path]])
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]]))

(def editors [:editors])
(def editors-path (path editors))

(def settings [:settings])
(def settings-path (path settings))
