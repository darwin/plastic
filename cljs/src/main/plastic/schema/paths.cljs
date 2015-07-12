(ns plastic.schema.paths
  (:require-macros [plastic.macros.logging :refer [log info warn error group group-end]])
  (:require [plastic.frame.middleware :refer [path]]))

(def editors [:editors])
(def editors-path (path editors))

(def settings [:settings])
(def settings-path (path settings))
