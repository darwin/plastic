(ns plastic.worker.editor.layout.analysis.defs
  (:require-macros [plastic.logging :refer [log info warn error group group-end]])
  (:require [plastic.util.helpers :as helpers]
            [plastic.worker.editor.layout.utils :as utils]
            [meld.zip :as zip]))

(defn extract-sym-doc [loc]
  (let [children (zip/child-locs loc)                                                                                 ; TODO: layout-utils/unwrap-metas
        first-string-loc (first (filter zip/string? children))
        first-symbol-loc (first (rest (filter zip/symbol? children)))]
    [(if first-symbol-loc
       [(zip/id first-symbol-loc) {:def-name? true}])
     (if first-string-loc
       [(zip/id first-string-loc) {:def-doc? true :selectable? true}])
     [(zip/id loc) {:def? true}]]))

(defn find-def-locs [loc]
  (filter utils/is-def? (take-while zip/good? (iterate zip/next loc))))

(defn analyze-defs [analysis loc]
  (let [def-locs (find-def-locs loc)
        defs-analysis (into {} (mapcat (fn [loc] (extract-sym-doc loc)) def-locs))]
    (helpers/deep-merge analysis defs-analysis)))