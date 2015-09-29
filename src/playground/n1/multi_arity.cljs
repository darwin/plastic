(ns n1.multi-arity)

(defn fn-multi "docstring"
  ([p1] 1)
  ([p1 p2] 2)
  ([p1 p2 & more] "more"))

(defn fn-single
  "docstring"
  [s1] 1)
