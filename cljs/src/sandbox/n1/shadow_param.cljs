(ns n1.shadow-param)

(defn fn1 [p]
  (fn fn2 [p]
    (use p)))