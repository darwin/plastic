(ns quark.dev.formatting
  (:require [devtools.core :as devtools]
            [devtools.format :as format]
            [rewrite-clj.node.seq :refer [SeqNode]]
            [clojure.string :as string]))

(defn devtools-formatting! []
  #_(extend-type SeqNode
    format/IDevtoolsFormat
    (-header [self] (format/template "span" "color:red;" (str "SEQ #" (:id self) (:tag self) " " (:value self))))
    (-has-body [_] true)
    (-body [self] nil)))
