(ns n1.unused-param)

(defn fn1 [unused _silenced _silenced-unused _ & more]
  (str _silenced))
