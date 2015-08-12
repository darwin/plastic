(ns n1.destructuring)

(let [{:keys [a b] :as x} (first)
      [c d [e a]] (first)]
  (log a b c d x))

(fn x [a [b c d [e {:keys [x y z] :as s}]]]
  (log a b c d e x y z s))

(try
  (something)
  (catch :default e
    (log e)))