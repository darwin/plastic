(ns n1.args)

(fn [a b c & rest]
  (log a b c rest))