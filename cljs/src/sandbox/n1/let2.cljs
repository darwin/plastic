(ns n1.let2)

(let [a 1
      a 2
      b (= (+ a a))
      a (= b)
      b (= a)
      b (= b)
      c (= (+ a b))
      d (= z)]
  (log a b c d e f g))