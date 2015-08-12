(ns n1.shortfn)

(def coll [1 2 3 4 5])

(map #(log % %1 %2 %3 %20 %50) coll)