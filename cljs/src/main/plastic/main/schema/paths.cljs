(ns plastic.main.schema.paths
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.main.frame :refer [path]]))

(def editors [:editors])
(def editors-path (path editors))

(def settings [:settings])
(def settings-path (path settings))